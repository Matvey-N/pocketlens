package com.pocketlens.assistant.service.tool;

import java.util.Map;

/**
 * A function the language model may call. Implementations run ordinary, user-scoped database queries; adding a
 * capability to the assistant means adding one implementation (Open/Closed principle).
 */
public interface AssistantTool {

    String name();

    String description();

    /** JSON Schema of the input object. */
    Map<String, Object> inputSchema();

    ToolResult execute(ToolContext context, ToolArguments arguments) throws ToolInputException;
}
