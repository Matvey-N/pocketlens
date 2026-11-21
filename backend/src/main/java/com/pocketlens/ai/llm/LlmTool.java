package com.pocketlens.ai.llm;

import java.util.Map;

/**
 * Definition of a function the model may call. {@code inputSchema} is a JSON Schema object.
 */
public record LlmTool(String name, String description, Map<String, Object> inputSchema) {
}
