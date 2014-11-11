package com.github.nexception.reviewassistant.models;

/**
 * A class tht represents a ReviewAssistant calculation.
 * The class contains review time and partitioning suggestions,
 * as well as a list of suitable reviewers.
 */
public class Calculation {
    public String changeId;
    public String patchId;
    public int totalTime;
    public int sessionTime;
    public int sessions;

    public Calculation(String changeId, String patchId, int totalTime, int sessionTime, int sessions) {
        this.changeId = changeId;
        this.patchId = patchId;
        this.totalTime = totalTime;
        this.sessions = sessionTime;
        this.sessions = sessions;
    }

    @Override
    public String toString() {
        return changeId + "+" + patchId;
    }
}
