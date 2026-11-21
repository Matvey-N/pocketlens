package com.pocketlens.ai.llm;

/**
 * Port to a large language model provider. The business logic depends only on this interface, so the provider
 * can be replaced (Dependency Inversion) and is mocked in tests.
 */
public interface LlmClient {

    /** {@code false} when no API key is configured; AI features are then disabled gracefully. */
    boolean isAvailable();

    /**
     * Sends a request and returns the model response.
     *
     * @throws AiUnavailableException when the provider cannot be reached or is not configured
     */
    LlmResponse send(LlmRequest request);
}
