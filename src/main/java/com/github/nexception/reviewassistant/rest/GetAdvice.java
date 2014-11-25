package com.github.nexception.reviewassistant.rest;

import com.github.nexception.reviewassistant.Storage;
import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.change.RevisionResource;
import com.google.inject.Inject;

/**
 * This rest view fetches a calculation and returns it. It is used by the front-end to
 * present the review suggestions to the users.
 */
public class GetAdvice implements RestReadView<RevisionResource> {

    private Storage storage;

    @Inject
    public GetAdvice(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Object apply(RevisionResource resource) throws AuthException, BadRequestException, ResourceConflictException {
        Calculation calculation = storage.fetchCalculation(resource.getPatchSet().getRevision().get());
        String advice = "<div>Reviewers should spend ";
        try {
            if (calculation.hourBlocks > 0) {
                advice += calculation.hourBlocks + " hours and ";
            }
            advice += (calculation.fiveMinuteBlocks * 5) + " minutes reviewing this change.</div>";
            if (calculation.sessions > 1) {
                advice += "<div>This could be split up in " + calculation.sessions + " sessions.</div>";
            }
        } catch (NullPointerException e) {
            advice = "<div>No advice exists for this change.</div>";
        }

        return advice;
    }
}
