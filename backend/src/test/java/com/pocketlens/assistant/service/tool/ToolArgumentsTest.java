package com.pocketlens.assistant.service.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ToolArgumentsTest {

    @Test
    void parsesValidPeriod() throws ToolInputException {
        ToolArguments.Period period = new ToolArguments(Map.of("from", "2026-10-01", "to", "2026-10-31")).period();
        assertThat(period.from()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(period).hasToString("2026-10-01 – 2026-10-31");
    }

    @Test
    void rejectsInvalidPeriods() {
        assertThatThrownBy(() -> new ToolArguments(Map.of("to", "2026-10-31")).period())
                .hasMessageContaining("'from' is required");
        assertThatThrownBy(() -> new ToolArguments(Map.of("from", "01.10.2026", "to", "2026-10-31")).period())
                .hasMessageContaining("YYYY-MM-DD");
        assertThatThrownBy(() -> new ToolArguments(Map.of("from", "2026-11-01", "to", "2026-10-31")).period())
                .hasMessageContaining("must not be after");
        assertThatThrownBy(() -> new ToolArguments(Map.of("from", "2020-01-01", "to", "2026-10-31")).period())
                .hasMessageContaining("two years");
    }

    @Test
    void readsOptionalValues() throws ToolInputException {
        Map<String, Object> values = new HashMap<>();
        values.put("order", "latest");
        values.put("limit", 5);
        values.put("month", "2026-10");
        values.put("blank", " ");
        ToolArguments args = new ToolArguments(values);

        assertThat(args.choice("order", Set.of("latest", "largest"), "largest")).isEqualTo("latest");
        assertThat(args.choice("missing", Set.of("a"), "a")).isEqualTo("a");
        assertThat(args.integer("limit", 10, 1, 20)).isEqualTo(5);
        assertThat(args.integer("missing", 10, 1, 20)).isEqualTo(10);
        assertThat(args.optionalMonth("month", null)).isEqualTo(YearMonth.of(2026, 10));
        assertThat(args.optionalMonth("missing", YearMonth.of(2026, 1))).isEqualTo(YearMonth.of(2026, 1));
        assertThat(args.optionalText("blank")).isNull();
        assertThat(new ToolArguments(null).raw()).isEmpty();
    }

    @Test
    void rejectsInvalidOptionalValues() {
        ToolArguments args = new ToolArguments(Map.of("order", "random", "limit", "ten", "big", 99, "month", "Oct"));
        assertThatThrownBy(() -> args.choice("order", Set.of("latest"), "latest")).isInstanceOf(ToolInputException.class);
        assertThatThrownBy(() -> args.integer("limit", 1, 1, 20)).isInstanceOf(ToolInputException.class);
        assertThatThrownBy(() -> args.integer("big", 1, 1, 20)).isInstanceOf(ToolInputException.class);
        assertThatThrownBy(() -> args.optionalMonth("month", null)).isInstanceOf(ToolInputException.class);
    }
}
