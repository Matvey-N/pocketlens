package com.pocketlens.ai.llm;

import java.util.List;

/**
 * Request to the model.
 *
 * @param forcedTool name of a tool the model must call (structured output), or {@code null}
 */
public record LlmRequest(String system, List<LlmMessage> messages, List<LlmTool> tools, String forcedTool,
                         int maxTokens) {

    public LlmRequest {
        messages = List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }
}
