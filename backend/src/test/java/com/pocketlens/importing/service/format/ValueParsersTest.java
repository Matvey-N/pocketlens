package com.pocketlens.importing.service.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ValueParsersTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "-12.50|.|-12.50",
            "1,234.56|.|1234.56",
            "€12.5|.|12.50",
            "(8.00)|.|-8.00",
            "1 234,56|,|1234.56",
            "-1.234,5|,|-1234.50",
            "+20|.|20.00"
    })
    void parsesAmounts(String raw, char separator, String expected) throws RowParseException {
        assertThat(ValueParsers.parseAmount(raw, separator)).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    void rejectsInvalidAmounts() {
        assertThatThrownBy(() -> ValueParsers.parseAmount("", '.')).isInstanceOf(RowParseException.class);
        assertThatThrownBy(() -> ValueParsers.parseAmount("abc", '.')).isInstanceOf(RowParseException.class);
        assertThatThrownBy(() -> ValueParsers.parseAmount("1.2.3", '.')).isInstanceOf(RowParseException.class);
    }

    @Test
    void optionalAmountDefaultsToZero() throws RowParseException {
        assertThat(ValueParsers.parseOptionalAmount(" ", '.')).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ValueParsers.parseOptionalAmount("5", '.')).isEqualByComparingTo("5");
    }

    @Test
    void parsesDatesWithAndWithoutTime() throws RowParseException {
        assertThat(ValueParsers.parseDate("2026-11-03", "yyyy-MM-dd")).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(ValueParsers.parseDate("2026-11-03 10:15:00", "yyyy-MM-dd HH:mm:ss"))
                .isEqualTo(LocalDate.of(2026, 11, 3));
        assertThatThrownBy(() -> ValueParsers.parseDate("03/11/2026", "yyyy-MM-dd"))
                .isInstanceOf(RowParseException.class);
        assertThatThrownBy(() -> ValueParsers.parseDate("", "yyyy-MM-dd")).isInstanceOf(RowParseException.class);
        assertThatThrownBy(() -> ValueParsers.parseDate("2026", "yyyy-MM-dd {")).isInstanceOf(RowParseException.class);
    }

    @Test
    void parsesCurrencyCodes() throws RowParseException {
        assertThat(ValueParsers.parseCurrency(" eur ")).isEqualTo("EUR");
        assertThatThrownBy(() -> ValueParsers.parseCurrency("EURO")).isInstanceOf(RowParseException.class);
        assertThatThrownBy(() -> ValueParsers.parseCurrency(null)).isInstanceOf(RowParseException.class);
    }
}
