package com.pocketlens.common.error;

/**
 * The requested resource does not exist or belongs to another user (both cases return 404 so that
 * the existence of other users' data is not revealed).
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String resource, Object id) {
        super(ErrorCode.NOT_FOUND, resource + " " + id + " was not found");
    }
}
