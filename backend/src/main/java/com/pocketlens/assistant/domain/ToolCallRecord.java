package com.pocketlens.assistant.domain;

import java.util.Map;

/**
 * A tool call made while answering, stored for transparency (US-4.2) and auditing.
 */
public record ToolCallRecord(String tool, Map<String, Object> arguments, String interpretation, boolean failed) {
}
