package com.github.nexception.reviewassistant;

import com.google.inject.Singleton;

/**
 * Created by simon on 2015-01-11.
 */
@Singleton
public class TestClass {

    private String message;

    public TestClass() {
        message = "Initiated";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
