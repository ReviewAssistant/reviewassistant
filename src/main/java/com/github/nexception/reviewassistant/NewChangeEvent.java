package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



class NewChangeEvent implements ChangeListener {

    private static final Logger log = LoggerFactory.getLogger(NewChangeEvent.class);
    private final GerritReviewAssistant.Factory gerritReviewAssistantFactory;
    private Storage storage;

    @Inject
    NewChangeEvent(final GerritReviewAssistant.Factory gerritReviewAssistantFactory, Storage storage) {
        this.gerritReviewAssistantFactory = gerritReviewAssistantFactory;
        this.storage = storage;
    }


    @Override
    public void onChangeEvent(ChangeEvent event) {
        if(!(event instanceof PatchSetCreatedEvent))
            return;
        PatchSetCreatedEvent e = (PatchSetCreatedEvent) event;
        log.info("Got new event:" + e.type + ": " + e.change.commitMessage);
        storage.storeCalculation(new Calculation(e.change.id, e.patchSet.revision, 1, 2, 3));
    }
}
