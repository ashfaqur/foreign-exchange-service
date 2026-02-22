package com.crewmeister.cmcodingchallenge.sync;

/**
 * Indicates that a sync request cannot start because another sync is running.
 */
public class SyncInProgressException extends RuntimeException {

    /**
     * Creates a sync-in-progress exception with the given message.
     *
     * @param message error message
     */
    public SyncInProgressException(String message) {
        super(message);
    }
}
