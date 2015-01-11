package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;
import com.google.gerrit.server.change.RevisionResource;

/**
 * The Cache interface is used to store and fetch calculations.
 */
public interface Cache {

    /**
     * Caches the provided calculation.
     * @param calculation the calculation object to be cached
     */
    public void storeCalculation(Calculation calculation);

    /**
     * Returns the calculation object with the matching RevisionResource.
     * @param resource the RevisionResource to fetch calculation from cache
     * @return a Calculation object if one is found, null otherwise
     */
    public Calculation fetchCalculation(RevisionResource resource);
}
