package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Algorithm {

    private static final Logger log = LoggerFactory.getLogger(Algorithm.class);

    public static Calculation calculate(PatchSetCreatedEvent event) {
        log.info("Got event: " + event.patchSet.revision);
        Calculation calculation = new Calculation();
        calculation.commitId = event.patchSet.revision;
        calculation.totalReviewTime = calculateReviewTime(event);

        return calculation;
    }

    private static int calculateReviewTime(PatchSetCreatedEvent event) {
        int lines = event.patchSet.sizeInsertions + event.patchSet.sizeDeletions;
        log.info("Insertions: " + event.patchSet.sizeInsertions);
        log.info("Deletions: " + event.patchSet.sizeDeletions);
        int minutes = (int)(lines*0.2);
        if(minutes < 5) {
            minutes = 5;
        }
        return minutes;
    }
}

