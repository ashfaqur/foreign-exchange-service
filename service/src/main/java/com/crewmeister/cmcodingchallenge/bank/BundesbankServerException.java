package com.crewmeister.cmcodingchallenge.bank;

public final class BundesbankServerException extends RuntimeException {
    public BundesbankServerException(int status, String body) {
        super("Bundesbank 5xx error. Status=" + status + ", body=" + body);
    }
}
