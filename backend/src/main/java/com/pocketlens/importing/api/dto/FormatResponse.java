package com.pocketlens.importing.api.dto;

import com.pocketlens.importing.service.format.StatementFormat;
import java.util.List;

public record FormatResponse(String id, String name, String delimiter, List<String> requiredColumns) {

    public static FormatResponse from(StatementFormat format) {
        return new FormatResponse(format.id(), format.displayName(), String.valueOf(format.delimiter()),
                format.requiredColumns());
    }
}
