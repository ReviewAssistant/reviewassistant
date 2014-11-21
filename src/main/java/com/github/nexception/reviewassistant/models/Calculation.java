package com.github.nexception.reviewassistant.models;

/**
 * A class that represents a ReviewAssistant calculation.
 * The class contains review time and review session suggestions.
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
