package com.github.nexception.reviewassistant.rest;

import com.github.nexception.reviewassistant.Storage;
import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.change.RevisionResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This rest view fetches a calculation and returns it. It is used by the front-end to
 * present the review suggestions to the users.
 */
@Singleton
public class GetAdvice implements RestReadView<RevisionResource> {

    private Storage storage;

    @Inject
    public GetAdvice(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Object apply(RevisionResource resource) throws AuthException, BadRequestException, ResourceConflictException {
        Calculation calculation = storage.fetchCalculation(resource.getPatchSet().getRevision().get());
        String advice = "<div id=\"reviewAssistant\" style=\"padding-top: 10px;\" ><strong>ReviewAssistant</strong>";
        advice += "<div>Reviewers should spend ";
        try {
            if (calculation.hours == 1) {
                advice += "<strong>" + calculation.hours + " hour</strong> ";
            } else if (calculation.hours > 1) {
                advice += "<strong>" + calculation.hours + " hours</strong> ";
            }
            if (calculation.hours > 0 && calculation.minutes > 0) {
                advice += "and ";
            }
            if (calculation.minutes > 0) {
                advice += "<strong>" + calculation.minutes + " minutes</strong> ";
            }
            advice += "reviewing this change.</div>";
            if (calculation.sessions > 1) {
                advice += "<div>This should be split up in <strong>" + calculation.sessions +
                        " to " + (calculation.sessions + 1) + " sessions</strong>.</div>";
            }
        } catch (NullPointerException e) {
            advice = "<div>No advice exists for this change.</div>";
        }
        advice += "</div>";
        return advice;
    }
}
