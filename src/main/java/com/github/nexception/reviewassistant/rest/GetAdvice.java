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
        String advice = "<div><p></p><b>ReviewAssistant</b>";
        advice += "<div>Reviewers should spend <b>";
        try {
            if (calculation.hourBlocks == 1) {
                advice += calculation.hourBlocks + " hour</b> and <b>";
            } else if(calculation.hourBlocks > 1) {
                advice += calculation.hourBlocks + " hours</b> and <b>";
            }
            advice += (calculation.fiveMinuteBlocks * 5) + " minutes</b> reviewing this change.</div>";
            if (calculation.sessions > 1) {
                advice += "<div>This should be split up in <b>" + calculation.sessions + " sessions</b>.</div>";
            }
        } catch (NullPointerException e) {
            advice = "<div>No advice exists for this change.</div>";
        }
        advice += "</div>";
        return advice;
    }
}
