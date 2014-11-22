package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;

/**
 * The Storage interface is used to store and fetch calculations.
 */
public interface Storage {

    /**
     * Stores the provided calculation.
     * @param calculation the calculation object to be stored
     */
    public void storeCalculation(Calculation calculation);

    /**
     * Returns the calculation object with the matching commit id.
     * @param commitId the commit id to look fetch
     * @return a Calculation object if one is found, null otherwise
     */
    public Calculation fetchCalculation(String commitId);
}
