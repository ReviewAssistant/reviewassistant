package com.github.nexception.reviewassistant;

import com.github.nexception.reviewassistant.models.Calculation;

public interface Storage {

    public void storeCalculation(Calculation calculation);

    public Calculation fetchCalculation(String identifier);
}
