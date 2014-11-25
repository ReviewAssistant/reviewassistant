package com.github.nexception.reviewassistant;

import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The change event listener listens to new commits and passes them on to the algorithm.
 */
class ChangeEventListener implements ChangeListener {

    private static final Logger log = LoggerFactory.getLogger(ChangeEventListener.class);
    private final ReviewAssistant.Factory reviewAssistantFactory;
    private Storage storage;
    private WorkQueue workQueue;

    @Inject
    ChangeEventListener(final ReviewAssistant.Factory reviewAssistantFactory, Storage storage, WorkQueue workQueue) {
        this.storage = storage;
        this.workQueue = workQueue;
        this.reviewAssistantFactory = reviewAssistantFactory;
    }


    @Override
    public void onChangeEvent(ChangeEvent event) {
        if(!(event instanceof PatchSetCreatedEvent))
            return;
        PatchSetCreatedEvent e = (PatchSetCreatedEvent) event;
        log.info("Received new commit: " + e.patchSet.revision);
        storage.storeCalculation(ReviewAssistant.calculate(e));
        final Runnable task = reviewAssistantFactory.create();
        workQueue.getDefaultQueue().submit(new Runnable() {
            @Override
            public void run() {
                task.run();
            }
        });
    }
}
