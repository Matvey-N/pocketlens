package com.pocketlens.assistant.service.tool;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Who is asking and when. Tools only ever read data of {@code userId}.
 */
public record ToolContext(UUID userId, LocalDate today) {
}
