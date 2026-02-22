package com.crewmeister.cmcodingchallenge.bank;

public final class BundesbankClientException extends RuntimeException {
    /**
     * Creates an exception for Bundesbank 4xx responses.
     *
     * @param status HTTP status code
     * @param body response body text
     */
    public BundesbankClientException(int status, String body) {
        super("Bundesbank 4xx error. Status=" + status + ", body=" + body);
    }
}
