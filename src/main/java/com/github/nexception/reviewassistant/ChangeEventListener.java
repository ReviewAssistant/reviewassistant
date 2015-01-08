package com.github.nexception.reviewassistant;

import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.PluginUser;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.WorkQueue;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.util.RequestContext;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The change event listener listens to new commits and passes them on to the algorithm.
 */
class ChangeEventListener implements ChangeListener {

    private static final Logger log = LoggerFactory.getLogger(ChangeEventListener.class);
    private final ReviewAssistant.Factory reviewAssistantFactory;
    private WorkQueue workQueue;
    private GitRepositoryManager repoManager;
    private SchemaFactory<ReviewDb> schemaFactory;
    private final ThreadLocalRequestContext tl;
    private ReviewDb db;
    private final PluginUser pluginUser;
    private final IdentifiedUser.GenericFactory identifiedUserFactory;
    private final PluginConfigFactory cfg;

    @Inject ChangeEventListener(final ReviewAssistant.Factory reviewAssistantFactory,
        final WorkQueue workQueue, final GitRepositoryManager repoManager,
        final SchemaFactory<ReviewDb> schemaFactory,
        final ThreadLocalRequestContext tl, final PluginUser pluginUser,
        final IdentifiedUser.GenericFactory identifiedUserFactory, final PluginConfigFactory cfg) {
        this.workQueue = workQueue;
        this.reviewAssistantFactory = reviewAssistantFactory;
        this.repoManager = repoManager;
        this.schemaFactory = schemaFactory;
        this.tl = tl;
        this.pluginUser = pluginUser;
        this.identifiedUserFactory = identifiedUserFactory;
        this.cfg = cfg;
    }

    @Override
    public void onChangeEvent(ChangeEvent changeEvent) {
        if (!(changeEvent instanceof PatchSetCreatedEvent)) {
            return;
        }
        PatchSetCreatedEvent event = (PatchSetCreatedEvent) changeEvent;
        log.debug("Received new commit: " + event.patchSet.revision);

        Project.NameKey projectName = new Project.NameKey(event.change.project);

        boolean autoAddReviewers = true;
        try {
            log.debug("Checking if autoAddReviewers is enabled");
            autoAddReviewers =
                cfg.getProjectPluginConfigWithInheritance(projectName, "reviewassistant")
                    .getBoolean("reviewers", "autoAddReviewers", true);
        } catch (NoSuchProjectException e) {
            log.error(e.getMessage(), e);
        }
        log.debug(
            autoAddReviewers ? "autoAddReviewers is enabled" : "autoAddReviewers is disabled");
        //TODO Reviewers should be suggested but not auto added?
        if (autoAddReviewers) {
            Repository repo;
            try {
                repo = repoManager.openRepository(projectName);
            } catch (RepositoryNotFoundException e) {
                log.error(e.getMessage(), e);
                return;
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return;
            }

            final ReviewDb reviewDb;
            final RevWalk walk = new RevWalk(repo);

            try {
                reviewDb = schemaFactory.open();
                try {
                    Change.Id changeId = new Change.Id(Integer.parseInt(event.change.number));
                    PatchSet.Id psId =
                        new PatchSet.Id(changeId, Integer.parseInt(event.patchSet.number));
                    PatchSet ps = reviewDb.patchSets().get(psId);
                    if (ps == null) {
                        log.warn("Could not find patch set " + psId.get());
                        return;
                    }
                    // psId.getParentKey = changeID
                    final Change change = reviewDb.changes().get(psId.getParentKey());
                    if (change == null) {
                        log.warn("Could not find change " + psId.getParentKey());
                        return;
                    }

                    RevCommit commit =
                        walk.parseCommit(ObjectId.fromString(event.patchSet.revision));

                    final Runnable task =
                        reviewAssistantFactory.create(commit, change, ps, repo, projectName);
                    workQueue.getDefaultQueue().submit(new Runnable() {
                        @Override
                        public void run() {
                            RequestContext old = tl.setContext(new RequestContext() {

                                @Override
                                public CurrentUser getCurrentUser() {
                                    if (!ReviewAssistant.realUser) {
                                        return pluginUser;
                                    } else {
                                        return identifiedUserFactory.create(change.getOwner());
                                    }
                                }

                                @Override
                                public Provider<ReviewDb> getReviewDbProvider() {
                                    return new Provider<ReviewDb>() {
                                        @Override
                                        public ReviewDb get() {
                                            if (db == null) {
                                                try {
                                                    db = schemaFactory.open();
                                                } catch (OrmException e) {
                                                    throw new ProvisionException(
                                                        "Cannot open ReviewDb", e);
                                                }
                                            }
                                            return db;
                                        }
                                    };
                                }
                            });
                            try {
                                task.run();
                            } finally {
                                tl.setContext(old);
                                if (db != null) {
                                    db.close();
                                    db = null;
                                }
                            }
                        }
                    });
                } catch (IncorrectObjectTypeException e) {
                    log.error(e.getMessage(), e);
                } catch (MissingObjectException e) {
                    log.error(e.getMessage(), e);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                } finally {
                    reviewDb.close();
                }
            } catch (OrmException e) {
                log.error(e.getMessage(), e);
            } finally {
                walk.release();
                repo.close();
            }
        }
    }
}
