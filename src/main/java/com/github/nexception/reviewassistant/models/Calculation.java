package com.github.nexception.reviewassistant.models;

/**
 * A class that represents a ReviewAssistant calculation.
 * The class contains review time and partitioning suggestions,
 * as well as a list of suitable reviewers.
 */
public class Calculation {
    public String commitId;
    public int totalReviewTime;
    public int sessionTime;
    public int sessions;

    public Calculation() {
        this.commitId = "nothing";
        this.totalReviewTime = 0;
        this.sessionTime = 0;
        this.sessions = 0;
    }

}
