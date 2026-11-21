package com.pocketlens.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionDetectorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);
    private final SubscriptionDetector detector = new SubscriptionDetector();

    @Test
    void detectsMonthlySubscriptionWithPriceIncrease() {
        List<PaymentPoint> payments = List.of(
                pay("SPOTIFY", "2026-08-03", "10.99"),
                pay("SPOTIFY", "2026-09-03", "10.99"),
                pay("SPOTIFY", "2026-10-03", "10.99"),
                pay("SPOTIFY", "2026-11-03", "11.99"));

        List<Subscription> result = detector.detect(payments, TODAY);

        assertThat(result).singleElement().satisfies(s -> {
            assertThat(s.cadence()).isEqualTo(Cadence.MONTHLY);
            assertThat(s.priceIncreased()).isTrue();
            assertThat(s.previousAmount()).isEqualByComparingTo("10.99");
            assertThat(s.nextExpectedDate()).isEqualTo(LocalDate.of(2026, 12, 3));
            assertThat(s.monthlyCost()).isEqualByComparingTo("11.99");
            assertThat(s.transactionIds()).hasSize(4);
        });
    }

    @Test
    void detectsWeeklyAndYearlyAndSortsByMonthlyCost() {
        List<PaymentPoint> payments = new ArrayList<>(List.of(
                pay("GYM", "2026-10-25", "10.00"), pay("GYM", "2026-11-01", "10.00"),
                pay("GYM", "2026-11-08", "10.00"), pay("GYM", "2026-11-15", "10.00"),
                pay("DOMAIN", "2024-11-01", "24.00"), pay("DOMAIN", "2025-11-01", "24.00"),
                pay("DOMAIN", "2026-11-01", "24.00")));

        List<Subscription> result = detector.detect(payments, TODAY);

        assertThat(result).extracting(Subscription::merchant).containsExactly("GYM", "DOMAIN");
        assertThat(result.get(0).monthlyCost()).isEqualByComparingTo("43.45");
        assertThat(result.get(1).monthlyCost()).isEqualByComparingTo("2.00");
    }

    @Test
    void ignoresIrregularOrDifferentAmountsOrStoppedPayments() {
        List<PaymentPoint> payments = List.of(
                pay("SHOP", "2026-08-01", "10"), pay("SHOP", "2026-08-20", "10"), pay("SHOP", "2026-10-30", "10"),
                pay("CAFE", "2026-08-03", "3"), pay("CAFE", "2026-09-03", "12"), pay("CAFE", "2026-10-03", "30"),
                pay("OLD", "2026-03-01", "5"), pay("OLD", "2026-04-01", "5"), pay("OLD", "2026-05-01", "5"),
                pay("TWICE", "2026-10-01", "5"), pay("TWICE", "2026-11-01", "5"));

        assertThat(detector.detect(payments, TODAY)).isEmpty();
    }

    @Test
    void toleratesOneIrregularInterval() {
        List<PaymentPoint> payments = List.of(
                pay("NETFLIX", "2026-06-05", "9.99"), pay("NETFLIX", "2026-07-05", "9.99"),
                pay("NETFLIX", "2026-08-25", "9.99"), pay("NETFLIX", "2026-09-25", "9.99"),
                pay("NETFLIX", "2026-10-25", "9.99"));
        assertThat(detector.detect(payments, TODAY)).hasSize(1);
    }

    private static PaymentPoint pay(String merchant, String date, String amount) {
        return new PaymentPoint(UUID.randomUUID(), merchant, "EUR", LocalDate.parse(date), new BigDecimal(amount));
    }
}
