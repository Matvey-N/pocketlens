package com.pocketlens.categorization.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class MerchantNormalizerTest {

    private final MerchantNormalizer normalizer = new MerchantNormalizer();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "CAFE BLOOM 0425|CAFE BLOOM",
            "PAYPAL *STEAM|PAYPAL STEAM",
            "Card payment to Lidl 1234 Vilnius|LIDL VILNIUS",
            "  spotify   p1f2a3b4c5  |SPOTIFY P1F2A3B4C5",
            "POS PURCHASE MAXIMA LT-123456|MAXIMA",
            "Netflix.com|NETFLIX.COM",
            "H&M STORE 0098|H&M STORE"
    })
    void normalisesDescriptions(String raw, String expected) {
        assertThat(normalizer.normalize(raw)).isEqualTo(expected);
    }

    @Test
    void keepsDescriptionWhenEverythingWouldBeRemoved() {
        assertThat(normalizer.normalize("123456")).isEqualTo("123456");
    }

    @Test
    void handlesNullAndLongValues() {
        assertThat(normalizer.normalize(null)).isEmpty();
        assertThat(normalizer.normalize("A".repeat(300))).hasSize(120);
    }
}
