package com.github.nexception.reviewassistant;

import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.WorkQueue;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;
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
    private Storage storage;
    private WorkQueue workQueue;
    private GitRepositoryManager repoManager;
    private SchemaFactory<ReviewDb> schemaFactory;
    private ReviewDb db;


    @Inject
    ChangeEventListener(final ReviewAssistant.Factory reviewAssistantFactory, Storage storage, WorkQueue workQueue, GitRepositoryManager repoManager, final SchemaFactory<ReviewDb> schemaFactory) {
        this.storage = storage;
        this.workQueue = workQueue;
        this.reviewAssistantFactory = reviewAssistantFactory;
        this.repoManager = repoManager;
        this.schemaFactory = schemaFactory;
    }


    @Override
    public void onChangeEvent(ChangeEvent changeEvent) {
        if(!(changeEvent instanceof PatchSetCreatedEvent))
            return;
        PatchSetCreatedEvent event = (PatchSetCreatedEvent) changeEvent;
        log.info("Received new commit: " + event.patchSet.revision);
        storage.storeCalculation(ReviewAssistant.calculate(event));
        //-----------------------------------------------------
        Project.NameKey projectName = new Project.NameKey(event.change.project);

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

        final ReviewDb db;
        final RevWalk rw = new RevWalk(repo);


        try{
            db = schemaFactory.open();
            try{
                Change.Id changeId = new Change.Id(Integer.parseInt(event.change.number));
                PatchSet.Id psId = new PatchSet.Id(changeId, Integer.parseInt(event.patchSet.number));
                PatchSet ps = db.patchSets().get(psId);
                if(ps == null){
                    log.warn("No patch set, " + psId.get());
                    return;
                }
                // psId.getParentKey = changeID
                Change change = db.changes().get(psId.getParentKey());
                if(change == null){
                    log.warn("No change" + psId.getParentKey());
                    return;
                }

                RevCommit commit = rw.parseCommit(ObjectId.fromString(event.patchSet.revision));

                final Runnable task = reviewAssistantFactory.create(commit, change, ps);
                workQueue.getDefaultQueue().submit(new Runnable() {
                    @Override
                    public void run() {
                        log.info("Run");
                        task.run();
                    }
                });

            } catch (IncorrectObjectTypeException e) {
                log.error(e.getMessage(), e);
            } catch (MissingObjectException e) {
                log.error(e.getMessage(), e);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            } finally{
                db.close();
            }

        } catch (OrmException e) {
            log.error(e.getMessage(), e);
        } finally {
            rw.release();
            repo.close();
        }
    }
}
