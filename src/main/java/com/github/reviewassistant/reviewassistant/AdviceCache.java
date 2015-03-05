package com.github.reviewassistant.reviewassistant;

import com.github.reviewassistant.reviewassistant.models.Calculation;
import com.google.gerrit.server.change.RevisionResource;

/**
 * The AdviceCache interface is used to store and fetch calculations.
 */
public interface AdviceCache {

    /**
     * Caches the provided calculation.
     *
     * @param calculation the calculation object to be cached
     */
    public void storeCalculation(Calculation calculation);

    /**
     * Returns the calculation object for the matching RevisionResource.
     *
     * @param resource the RevisionResource to fetch calculation for from the cache
     * @return a Calculation object if one is found, null otherwise
     */
    public Calculation fetchCalculation(RevisionResource resource);
}
