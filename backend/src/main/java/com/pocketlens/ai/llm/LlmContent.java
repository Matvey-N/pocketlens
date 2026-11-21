package com.pocketlens.ai.llm;

import java.util.Map;

/**
 * One block of an LLM message: text, a tool call requested by the model, or the result of a tool call.
 */
public sealed interface LlmContent {

    /** Plain text. */
    record Text(String text) implements LlmContent {
    }

    /** The model asks the backend to run a tool with the given input. */
    record ToolUse(String id, String name, Map<String, Object> input) implements LlmContent {
        public ToolUse {
            input = input == null ? Map.of() : input;
        }
    }

    /** Result of a tool call, returned to the model as JSON text. */
    record ToolResult(String toolUseId, String content, boolean isError) implements LlmContent {
    }
}
