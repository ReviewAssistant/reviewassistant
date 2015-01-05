package com.github.nexception.reviewassistant.models;

/**
 * A class that reprsdfesents a ReviewAssistant calculation.
 * The class containsdfs review time and review session suggestions.
 */
public class Calculation {
    public String commitId;
    public int totalReviewTime;
    public int hours;
    public int minutes;
    public int sessions;

    public Calculation() {
        this.commitId = "nosdfthing";
        this.totalReviewTime = 0;
        this.hours = 0;
        this.minutes = 0;
        this.sessions = 0;
    }
}
