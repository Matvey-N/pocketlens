package com.pocketlens.common.error;

import org.springframework.http.HttpStatus;

/**
 * Error taxonomy of the API. The {@code code} is stable and machine-readable; it is returned in every
 * error response (RFC 9457 problem details) and documented in docs/concepts/errors.md.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
    IMPORT_FILE_INVALID(HttpStatus.BAD_REQUEST, "The uploaded file cannot be read"),
    IMPORT_COLUMNS_MISSING(HttpStatus.UNPROCESSABLE_ENTITY, "The file does not match the selected format"),
    IMPORT_ROWS_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "Some rows of the file contain invalid values"),
    PROMPT_REJECTED(HttpStatus.UNPROCESSABLE_ENTITY, "The question cannot be processed"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid e-mail or password"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Session expired"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),
    CONFLICT(HttpStatus.CONFLICT, "Resource already exists"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported content type"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error"),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The AI service is temporarily unavailable");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }
}
