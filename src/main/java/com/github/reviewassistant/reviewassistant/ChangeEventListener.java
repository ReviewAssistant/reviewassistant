package com.github.reviewassistant.reviewassistant;

import com.google.gerrit.common.EventListener;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.PluginUser;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.Event;
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
class ChangeEventListener implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(ChangeEventListener.class);
    private final ReviewAssistant.Factory reviewAssistantFactory;
    private final ThreadLocalRequestContext tl;
    private final IdentifiedUser.GenericFactory identifiedUserFactory;
    private final PluginConfigFactory cfg;
    private final String pluginName;
    private WorkQueue workQueue;
    private GitRepositoryManager repoManager;
    private SchemaFactory<ReviewDb> schemaFactory;
    private ReviewDb db;

    @Inject ChangeEventListener(final ReviewAssistant.Factory reviewAssistantFactory,
        final WorkQueue workQueue, final GitRepositoryManager repoManager,
        final SchemaFactory<ReviewDb> schemaFactory, final ThreadLocalRequestContext tl,
        final PluginUser pluginUser, final IdentifiedUser.GenericFactory identifiedUserFactory,
        final PluginConfigFactory cfg, @PluginName String pluginName) {
        this.workQueue = workQueue;
        this.reviewAssistantFactory = reviewAssistantFactory;
        this.repoManager = repoManager;
        this.schemaFactory = schemaFactory;
        this.tl = tl;
        this.identifiedUserFactory = identifiedUserFactory;
        this.cfg = cfg;
        this.pluginName = pluginName;
    }

    @Override public void onEvent(Event changeEvent) {
        if (!(changeEvent instanceof PatchSetCreatedEvent)) {
            return;
        }
        PatchSetCreatedEvent event = (PatchSetCreatedEvent) changeEvent;
        log.debug("Received new commit: {}", event.patchSet.revision);

        Project.NameKey projectName = new Project.NameKey(event.change.project);

        boolean autoAddReviewers = true;
        try {
            log.debug("Checking if autoAddReviewers is enabled");
            autoAddReviewers =
                cfg.getProjectPluginConfigWithInheritance(projectName, pluginName)
                    .getBoolean("reviewers", "autoAddReviewers", true);
        } catch (NoSuchProjectException e) {
            log.error("Could not find project {}", projectName);
        }
        log.debug(
            autoAddReviewers ? "autoAddReviewers is enabled" : "autoAddReviewers is disabled");
        if (autoAddReviewers) {
            try (Repository repo = repoManager.openRepository(projectName)) {
                final ReviewDb reviewDb;
                try (RevWalk walk = new RevWalk(repo)) {
                    reviewDb = schemaFactory.open();
                    try {
                        Change.Id changeId = new Change.Id(Integer.parseInt(event.change.number));
                        PatchSet.Id psId =
                            new PatchSet.Id(changeId, Integer.parseInt(event.patchSet.number));
                        PatchSet ps = reviewDb.patchSets().get(psId);
                        if (ps == null) {
                            log.warn("Could not find patch set {}", psId.get());
                            return;
                        }
                        // psId.getParentKey = changeID
                        final Change change = reviewDb.changes().get(psId.getParentKey());
                        if (change == null) {
                            log.warn("Could not find change {}", psId.getParentKey());
                            return;
                        }

                        RevCommit commit =
                            walk.parseCommit(ObjectId.fromString(event.patchSet.revision));

                        final Runnable task =
                            reviewAssistantFactory.create(commit, change, ps, repo, projectName);
                        workQueue.getDefaultQueue().submit(new Runnable() {
                            @Override public void run() {
                                RequestContext old = tl.setContext(new RequestContext() {

                                    @Override public CurrentUser getCurrentUser() {
                                        return identifiedUserFactory.create(change.getOwner());
                                    }

                                    @Override public Provider<ReviewDb> getReviewDbProvider() {
                                        return new Provider<ReviewDb>() {
                                            @Override public ReviewDb get() {
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
                    } catch (IOException e) {
                        log.error("Could not get commit for revision {}: {}", event.patchSet.revision,
                            e);
                    } finally {
                        reviewDb.close();
                    }
                } catch (OrmException e) {
                    log.error("Could not open review database: {}", e);
                }
            } catch (IOException e) {
                log.error("Could not open repository for {}", projectName);
                return;
            }
        }
    }
}
