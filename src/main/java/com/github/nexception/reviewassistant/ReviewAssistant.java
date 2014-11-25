package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for calculating recommended review time and
 * recommended reviewers.
 */
public class ReviewAssistant {

    private static final Logger log = LoggerFactory.getLogger(ReviewAssistant.class);

    /**
     * Returns a Calculation object with all relevant information
     * regarding a review for a patch set.
     * @param event the event for a patch set
     * @return      the Calculation object for a review
     */
    public static Calculation calculate(PatchSetCreatedEvent event) {
        log.info("Received event: " + event.patchSet.revision);
        Calculation calculation = new Calculation();
        calculation.commitId = event.patchSet.revision;
        calculation.totalReviewTime = calculateReviewTime(event);
        calculation.hourBlocks = calculateHourBlocks(calculateReviewTime(event));
        calculation.fiveMinuteBlocks = calculateFiveMinuteBlocks(calculateReviewTime(event));
        calculation.sessions = calculateReviewSessions(calculateReviewTime(event));
        calculation.sessionTime = calculateReviewTime(event) / calculateReviewSessions(calculateReviewTime(event));

        return calculation;
    }

    /**
     * Returns the total amount of time in minutes recommended for a review.
     * Adds all line insertions and deletions for a patch set and calculates
     * the amount of minutes needed.
     * This calculation is based on the optimum review rate of 5 LOC in 1 minute.
     * @param event the event for a patch set
     * @return      the total amount of time recommended for a review
     */
    private static int calculateReviewTime(PatchSetCreatedEvent event) {
        int lines = event.patchSet.sizeInsertions + Math.abs(event.patchSet.sizeDeletions);
        log.info("Insertions: " + event.patchSet.sizeInsertions);
        log.info("Deletions: " + event.patchSet.sizeDeletions);
        int minutes = (int) Math.ceil(lines / 5);
        if(minutes < 5) {
            minutes = 5;
        }
        return minutes;
    }

    /**
     * Returns the recommended amount of review sessions for a review.
     * Divides the total amount of review time up in 60 minute sessions.
     * @param minutes the total amount of time recommended for a review
     * @return        the recommended amount of review sessions
     */
    private static int calculateReviewSessions(int minutes) {
        int sessions = Math.round(minutes / 60);
        if(sessions < 1) {
            sessions = 1;
        }
        return sessions;
    }


    /**
     * Returns the amount of hour blocks the total review time can be
     * divided into.
     * @param minutes the total amount of time recommended for a review
     * @return        the amount of hour blocks
     */
    private static int calculateHourBlocks(int minutes) {
        int hourBlocks = minutes / 60;
        return hourBlocks;
    }

    /**
     * Returns the amount of 5 minute blocks the review time can be
     * divided into, after it has been divided into hour blocks.
     * @param minutes the total amount of time recommended for a review
     * @return        the amount of 5 minute blocks
     */
    private static int calculateFiveMinuteBlocks(int minutes) {
        int fiveMinuteBlocks = (int) Math.ceil((minutes % 60) / 5.0);
        return fiveMinuteBlocks;
    }
}

