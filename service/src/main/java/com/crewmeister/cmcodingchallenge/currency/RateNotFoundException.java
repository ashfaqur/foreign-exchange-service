package com.crewmeister.cmcodingchallenge.currency;

public class RateNotFoundException extends RuntimeException{
    public RateNotFoundException(String message) {
        super(message);
    }
}
