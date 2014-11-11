package com.github.nexception.reviewassistant.models;

/**
 * A class that represents a ReviewAssistant calculation.
 * The class contains review time and partitioning suggestions,
 * as well as a list of suitable reviewers.
 */
public class Calculation {
    public String commitId;
    public int totalTime;
    public int sessionTime;
    public int sessions;

    public Calculation(String commitId, int totalTime, int sessionTime, int sessions) {
        this.commitId = commitId;
        this.totalTime = totalTime;
        this.sessionTime = sessionTime;
        this.sessions = sessions;
    }

    @Override
    public String toString() {
        return commitId;
    }
}
