package com.github.reviewassistant.reviewassistant.rest;

import com.github.reviewassistant.reviewassistant.AdviceCache;
import com.github.reviewassistant.reviewassistant.models.Calculation;
import com.google.gerrit.extensions.restapi.RestApiException;
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

    @Override public String apply(RevisionResource resource) throws RestApiException {
        Calculation calculation = adviceCache.fetchCalculation(resource);
        if (calculation == null) {
            return "Could not get advice for this change.";
        }
        StringBuilder advice = new StringBuilder("Reviewers should spend <strong>");
        if (calculation.hours >= 1) {
            advice.append(calculation.hours)
              .append(" hour")
              .append(calculation.hours > 1 ? "s" : "");
        }
        if (calculation.hours > 0 && calculation.minutes > 0) {
            advice.append(" and ");
        }
        if (calculation.minutes > 0) {
            advice.append(calculation.minutes)
              .append(" minute")
              .append(calculation.minutes > 1 ? "s" : "");
        }
        advice.append("</strong> reviewing this change.");
        if (calculation.hours >= 1) {
            advice.append("<p>This should be split up in <strong>")
              .append(calculation.sessions)
              .append(" to ")
              .append(calculation.sessions + 1)
              .append(" sessions</strong>.");
        }

        return advice.toString();
    }
}
