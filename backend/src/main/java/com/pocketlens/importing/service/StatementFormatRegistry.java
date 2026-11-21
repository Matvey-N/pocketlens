package com.pocketlens.importing.service;

import com.pocketlens.common.error.ApiException;
import com.pocketlens.common.error.ErrorCode;
import com.pocketlens.importing.service.format.StatementFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * All built-in statement formats (every {@link StatementFormat} bean).
 */
@Component
public class StatementFormatRegistry {

    private final Map<String, StatementFormat> formats;

    public StatementFormatRegistry(List<StatementFormat> formats) {
        this.formats = formats.stream().collect(Collectors.toMap(StatementFormat::id, Function.identity()));
    }

    public List<StatementFormat> all() {
        return formats.values().stream().sorted(Comparator.comparing(StatementFormat::displayName)).toList();
    }

    public Optional<StatementFormat> find(String id) {
        return Optional.ofNullable(formats.get(id));
    }

    public StatementFormat get(String id) {
        return find(id).orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                "Unknown format '" + id + "'. Available: " + String.join(", ", formats.keySet())));
    }
}
