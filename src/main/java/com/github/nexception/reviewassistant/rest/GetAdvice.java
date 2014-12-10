package com.github.nexception.reviewassistant.rest;

import com.github.nexception.reviewassistant.ReviewAssistant;
import com.github.nexception.reviewassistant.Storage;
import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestApiException;
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
    private GerritApi gApi;
    private ChangeApi cApi;

    @Inject
    public GetAdvice(GerritApi gApi, Storage storage) {
        this.storage = storage;
        this.gApi = gApi;
    }

    @Override
    public Object apply(RevisionResource resource) throws AuthException, BadRequestException, ResourceConflictException {
        Calculation calculation = storage.fetchCalculation(resource.getPatchSet().getRevision().get());
        String advice = "<div id=\"reviewAssistant\" style=\"padding-top: 10px;\" ><strong>ReviewAssistant</strong>";
        advice += "<div>Reviewers should spend <strong>";
        // Still missing gif-file
        if(calculation == null) {
            try {
                cApi = gApi.changes().id(resource.getChange().getChangeId());
                ChangeInfo info = cApi.get();
                storage.storeCalculation(ReviewAssistant.calculate(info));
                calculation = storage.fetchCalculation(resource.getPatchSet().getRevision().get()); // Fetch file again
            } catch (RestApiException e) {
                e.printStackTrace();   // Should make use of log
            }
        }

        try {
            if (calculation.hours == 1) {
                advice += calculation.hours + " hour";
            } else if (calculation.hours > 1) {
                advice += calculation.hours + " hours";
            }
            if (calculation.hours > 0 && calculation.minutes > 0) {
                advice += " and ";
            }
            if (calculation.minutes > 0) {
                advice += calculation.minutes + " minutes";
            }
            advice += "</strong> reviewing this change.</div>";
            if (calculation.hours >= 1) {
                advice += "<div>This should be split up in <strong>" + calculation.sessions +
                        " to " + (calculation.sessions + 1) + " sessions</strong>.</div>";
            }
        } catch (NullPointerException e) {      // Probably not needed anymore, or insert gif here
            advice = "<div>No advice exists for this change.</div>";
        }
        advice += "</div>";
        return advice;
    }
}