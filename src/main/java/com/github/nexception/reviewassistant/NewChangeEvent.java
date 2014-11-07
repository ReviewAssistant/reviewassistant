package com.github.nexception.reviewassistant;

import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



class NewChangeEvent implements ChangeListener {

    private static final Logger log = LoggerFactory.getLogger(NewChangeEvent.class);
    private final GerritReviewAssistant.Factory gerritReviewAssistantFactory;

    @Inject
    NewChangeEvent(final GerritReviewAssistant.Factory gerritReviewAssistantFactory) {
        this.gerritReviewAssistantFactory = gerritReviewAssistantFactory;
    }


    @Override
    public void onChangeEvent(ChangeEvent event) {
        if(!(event instanceof PatchSetCreatedEvent))
            return;
        PatchSetCreatedEvent e = (PatchSetCreatedEvent) event;
        log.info("Got new event:" + e.type );
        log.info(e.change.commitMessage);

    }
}
