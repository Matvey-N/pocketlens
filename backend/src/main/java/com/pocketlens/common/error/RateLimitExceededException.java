package com.pocketlens.common.error;

import java.util.Map;

/**
 * The caller exceeded a request quota. {@code retryAfterSeconds} is also sent in the Retry-After header.
 */
public class RateLimitExceededException extends ApiException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super(ErrorCode.RATE_LIMITED, "Request limit reached. Try again in " + retryAfterSeconds + " s",
                Map.of("retryAfterSeconds", retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
