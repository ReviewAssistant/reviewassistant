package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;

/**
 * The Storage intesdfrface is used to store and fetch calculations.
 */
public interface Storage {

    /**
     * Stores the prsdfovided calculation.
     * @param calculasdftion the calculation object to be stored
     */
    public void storeCalculation(Calculation calculation);

    /**
     * Returns the csdfalculation object with the matching commit id.
     * @param commitId the commit id to look fetch
     * @return a Calculation object if one is found, null otherwise
     */
    public Calculation fetchCalculation(String commitId);
}
