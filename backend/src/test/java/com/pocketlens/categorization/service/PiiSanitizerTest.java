package com.pocketlens.categorization.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiSanitizerTest {

    private final PiiSanitizer sanitizer = new PiiSanitizer();

    @Test
    void removesIbanCardEmailPhoneAndAccountNumbers() {
        String text = "Transfer LT12 1000 0111 0100 1000 card 4111 1111 1111 1111 mail john.doe@mail.com "
                + "phone +370 612 34567 ref 99887766 masked 5168****1234";
        String result = sanitizer.sanitize(text);
        assertThat(result)
                .doesNotContain("LT12", "4111", "john.doe", "612 34567", "99887766", "5168")
                .contains("[IBAN]", "[EMAIL]", "[NUMBER]", "[CARD]");
        assertThat(sanitizer.containsPii(result)).isFalse();
    }

    @Test
    void keepsOrdinaryMerchantNames() {
        assertThat(sanitizer.sanitize("CAFE BLOOM")).isEqualTo("CAFE BLOOM");
        assertThat(sanitizer.sanitize(null)).isEmpty();
    }

    @Test
    void detectsPii() {
        assertThat(sanitizer.containsPii("DE89370400440532013000")).isTrue();
        assertThat(sanitizer.containsPii("SPOTIFY")).isFalse();
        assertThat(sanitizer.containsPii(null)).isFalse();
    }
}
