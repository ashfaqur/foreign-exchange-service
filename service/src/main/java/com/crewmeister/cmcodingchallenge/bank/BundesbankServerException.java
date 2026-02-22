package com.crewmeister.cmcodingchallenge.bank;

public final class BundesbankServerException extends RuntimeException {
    /**
     * Creates an exception for Bundesbank 5xx responses.
     *
     * @param status HTTP status code
     * @param body response body text
     */
    public BundesbankServerException(int status, String body) {
        super("Bundesbank 5xx error. Status=" + status + ", body=" + body);
    }
}
