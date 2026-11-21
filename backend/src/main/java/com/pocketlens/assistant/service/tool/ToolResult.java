package com.pocketlens.assistant.service.tool;

import java.util.List;
import java.util.UUID;

/**
 * Output of a tool.
 *
 * @param data           JSON-serialisable result sent to the model (no personal identifiers)
 * @param interpretation human-readable description of what was queried (shown to the user, US-4.2)
 * @param transactionIds transactions behind the figures (US-4.3)
 */
public record ToolResult(Object data, String interpretation, List<UUID> transactionIds) {

    public ToolResult {
        transactionIds = transactionIds == null ? List.of() : List.copyOf(transactionIds);
    }
}
