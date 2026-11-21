package com.pocketlens.importing.service;

import static com.pocketlens.support.TestData.OTHER_USER_ID;
import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.pocketlens.importing.service.format.ParsedTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FingerprintCalculatorTest {

    private final FingerprintCalculator calculator = new FingerprintCalculator();
    private final ParsedTransaction coffee = new ParsedTransaction(LocalDate.of(2026, 11, 1), new BigDecimal("-3.20"),
            "EUR", "Cafe  Bloom");

    @Test
    void identicalRowsInOneFileGetDifferentKeysButReimportGivesSameKeys() {
        List<String> first = calculator.fingerprints(USER_ID, List.of(coffee, coffee));
        List<String> second = calculator.fingerprints(USER_ID, List.of(coffee, coffee));

        assertThat(first).hasSize(2).doesNotHaveDuplicates();
        assertThat(second).isEqualTo(first);
        assertThat(first.get(0)).hasSize(64);
    }

    @Test
    void keysDependOnUserAndNormalisedDescription() {
        ParsedTransaction sameDifferentSpacing = new ParsedTransaction(coffee.date(), coffee.amount(), "EUR",
                "CAFE BLOOM");
        assertThat(calculator.fingerprints(USER_ID, List.of(coffee)))
                .isEqualTo(calculator.fingerprints(USER_ID, List.of(sameDifferentSpacing)))
                .isNotEqualTo(calculator.fingerprints(OTHER_USER_ID, List.of(coffee)));
    }
}
