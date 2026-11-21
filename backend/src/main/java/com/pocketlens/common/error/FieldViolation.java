package com.pocketlens.common.error;

/**
 * One invalid field of a request.
 */
public record FieldViolation(String field, String message) {
}
