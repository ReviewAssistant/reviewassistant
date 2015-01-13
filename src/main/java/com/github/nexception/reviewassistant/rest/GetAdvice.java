package com.github.nexception.reviewassistant.rest;

import com.github.nexception.reviewassistant.AdviceCache;
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
@Singleton public class GetAdvice implements RestReadView<RevisionResource> {

    private AdviceCache adviceCache;

    @Inject public GetAdvice(AdviceCache adviceCache) {
        this.adviceCache = adviceCache;
    }

    @Override public Object apply(RevisionResource resource)
        throws AuthException, BadRequestException, ResourceConflictException {
        Calculation calculation = adviceCache.fetchCalculation(resource);
        String advice =
            "<div id=\"reviewAssistant\" style=\"padding-top: 10px;\" ><strong>ReviewAssistant</strong>";
        if (calculation != null) {
            advice += "<div>Reviewers should spend <strong>";
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
        } else {
            advice += "<div>Could not get advice for this change.</div>";
        }

        advice += "</div>";
        return advice;
    }
}
