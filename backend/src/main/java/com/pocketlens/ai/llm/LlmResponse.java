package com.pocketlens.ai.llm;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Response of the model with token usage.
 */
public record LlmResponse(List<LlmContent> content, String stopReason, int inputTokens, int outputTokens) {

    public static final String STOP_TOOL_USE = "tool_use";

    public LlmResponse {
        content = List.copyOf(content);
    }

    public boolean wantsTools() {
        return STOP_TOOL_USE.equals(stopReason) || !toolUses().isEmpty();
    }

    public List<LlmContent.ToolUse> toolUses() {
        return content.stream()
                .filter(LlmContent.ToolUse.class::isInstance)
                .map(LlmContent.ToolUse.class::cast)
                .toList();
    }

    public String text() {
        return content.stream()
                .filter(LlmContent.Text.class::isInstance)
                .map(c -> ((LlmContent.Text) c).text())
                .collect(Collectors.joining("\n"))
                .strip();
    }
}
