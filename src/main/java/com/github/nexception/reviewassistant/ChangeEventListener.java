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
  // private final GerritReviewAssistan.Factory gerritReviewAssistantFactory;
    private Storage storage;
    private WorkQueue workQueue;

    @Inject
  //  ChangeEventListener(final GerritReviewAssistant.Factory gerritReviewAssistantFactory, Storage storage) {
    ChangeEventListener(Storage storage, WorkQueue workQueue) {
       // this.gerritReviewAssistantFactory = gerritReviewAssistantFactory;
        this.storage = storage;
        this.workQueue = workQueue;
    }


    @Override
    public void onChangeEvent(ChangeEvent event) {
        if(!(event instanceof PatchSetCreatedEvent))
            return;
        PatchSetCreatedEvent e = (PatchSetCreatedEvent) event;
        log.info("Received new commit: " + e.patchSet.revision);
        storage.storeCalculation(ReviewAssistant.calculate(e));
    }
}
