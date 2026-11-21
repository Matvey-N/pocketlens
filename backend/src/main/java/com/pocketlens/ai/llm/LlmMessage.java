package com.pocketlens.ai.llm;

import java.util.List;

/**
 * A message of the conversation with the model.
 */
public record LlmMessage(Role role, List<LlmContent> content) {

    public enum Role { USER, ASSISTANT }

    public LlmMessage {
        content = List.copyOf(content);
    }

    public static LlmMessage user(String text) {
        return new LlmMessage(Role.USER, List.of(new LlmContent.Text(text)));
    }

    public static LlmMessage assistant(String text) {
        return new LlmMessage(Role.ASSISTANT, List.of(new LlmContent.Text(text)));
    }
}
