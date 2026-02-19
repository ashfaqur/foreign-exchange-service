package com.crewmeister.cmcodingchallenge.bank;

public final class BundesbankClientException extends RuntimeException {
    public BundesbankClientException(int status, String body) {
        super("Bundesbank 4xx error. Status=" + status + ", body=" + body);
    }
}
