package com.pocketlens.common.error;

/**
 * The request conflicts with the current state, e.g. a duplicate unique value.
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
