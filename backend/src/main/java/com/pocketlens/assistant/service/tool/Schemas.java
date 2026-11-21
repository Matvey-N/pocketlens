package com.pocketlens.assistant.service.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small helpers to build JSON Schemas for tool inputs.
 */
final class Schemas {

    static final Map<String, Object> DATE = Map.of("type", "string", "format", "date",
            "description", "Date in the format YYYY-MM-DD");
    static final Map<String, Object> CATEGORY = Map.of("type", "string",
            "description", "Exact category name from the user's categories, or 'Uncategorised'");
    static final Map<String, Object> MERCHANT = Map.of("type", "string",
            "description", "Part of the merchant name, e.g. 'SPOTIFY'");

    private Schemas() {
    }

    static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    static Map<String, Object> period(Map<String, Object> extra) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("from", DATE);
        properties.put("to", DATE);
        properties.putAll(extra);
        return object(properties, List.of("from", "to"));
    }
}
