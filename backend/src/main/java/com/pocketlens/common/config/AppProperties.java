package com.pocketlens.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed application configuration. Every value can be overridden with an environment variable
 * (see application.yml), so no secret or environment-specific value is hardcoded.
 */
@Validated
@ConfigurationProperties(prefix = "pocketlens")
public record AppProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Ai ai,
        @Valid @NotNull Assistant assistant,
        @Valid @NotNull Imports imports) {

    /** Settings of the access and refresh tokens. */
    public record Jwt(
            @NotBlank String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl,
            boolean secureCookie) {
    }

    /** Origins allowed to call the API from a browser (only needed when the SPA runs on another origin). */
    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }

    /** Connection to the LLM provider. An empty API key disables all AI features gracefully. */
    public record Ai(
            String apiKey,
            @NotBlank String baseUrl,
            @NotBlank String model,
            @Min(64) int maxTokens,
            @NotNull Duration timeout,
            @DecimalMin("0.0") @DecimalMax("1.0") double confidenceThreshold,
            @Min(1) int categorizationBatchSize) {

        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /** Limits of the AI assistant (NFR-S4). */
    public record Assistant(
            @Min(1) int maxQuestionLength,
            @Min(1) int requestsPerMinute,
            @Min(1) int maxToolIterations,
            @Min(1) int historyMessages) {
    }

    /** Limits of the statement import (UC-1, step 3). */
    public record Imports(@Min(1) long maxFileSizeBytes, @Min(1) int maxRows) {
    }
}
