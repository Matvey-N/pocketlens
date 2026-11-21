package com.pocketlens.ai.llm;

/**
 * Used when no API key is configured: the application works, AI features answer 503 AI_UNAVAILABLE.
 */
public class DisabledLlmClient implements LlmClient {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public LlmResponse send(LlmRequest request) {
        throw new AiUnavailableException("AI features are not configured on this server");
    }
}
