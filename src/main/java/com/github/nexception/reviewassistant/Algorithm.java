package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for calculating recommended review time and
 * recommended reviewers.
 */
public class Algorithm {

    private static final Logger log = LoggerFactory.getLogger(Algorithm.class);

    /**
     * Returns a Calculation object with all relevant information regarding
     * a review for a patch set.
     * @param event the event for a patch set
     * @return      the Calculation object for a review
     */
    public static Calculation calculate(PatchSetCreatedEvent event) {
        log.info("Received event: " + event.patchSet.revision);
        Calculation calculation = new Calculation();
        calculation.commitId = event.patchSet.revision;
        calculation.totalReviewTime = calculateReviewTime(event);

        return calculation;
    }

    /**
     * Returns the total amount of time in minutes recommended for a review.
     * Adds all line insertions and deletions for a patch set and calculates
     * the amount of minutes needed.
     * @param event the event for a patch set
     * @return      the total amount of time recommended for a review
     */
    private static int calculateReviewTime(PatchSetCreatedEvent event) {
        int lines = event.patchSet.sizeInsertions - event.patchSet.sizeDeletions;
        log.info("Insertions: " + event.patchSet.sizeInsertions);
        log.info("Deletions: " + event.patchSet.sizeDeletions);
        int minutes = (int) (lines * 0.2);
        if(minutes < 5) {
            minutes = 5;
        }
        return minutes;
    }
}

