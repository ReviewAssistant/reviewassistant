package com.github.nexception.reviewassistant.models;

/**
 * A class that represents a ReviewAssistant calculation.
 * The class contains review time and review session suggestions.
 */
public class Calculation {
    public String commitId;
    public int totalReviewTime;
    public int hours;
    public int minutes;
    public int sessions;

    public Calculation() {
        this.commitId = "nothing";
        this.totalReviewTime = 0;
        this.hours = 0;
        this.minutes = 0;
        this.sessions = 0;
    }
}
