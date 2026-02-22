package com.crewmeister.cmcodingchallenge.currency;

public class RateNotFoundException extends RuntimeException {
    /**
     * Creates an exception for missing rate data.
     *
     * @param message error message
     */
    public RateNotFoundException(String message) {
        super(message);
    }
}
