package com.pocketlens.subscription.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Detects recurring payments: at least three payments to the same merchant in the same currency with regular
 * intervals (weekly, monthly or yearly) and similar amounts (within 25 % of the median).
 */
@Component
public class SubscriptionDetector {

    static final int MIN_OCCURRENCES = 3;
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.25");
    private static final BigDecimal PRICE_INCREASE_THRESHOLD = new BigDecimal("1.01");
    private static final BigDecimal WEEKS_PER_MONTH = new BigDecimal("4.345");
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    public List<Subscription> detect(List<PaymentPoint> payments, LocalDate today) {
        Map<String, List<PaymentPoint>> groups = payments.stream()
                .collect(Collectors.groupingBy(p -> p.merchant() + "|" + p.currency()));
        List<Subscription> result = new ArrayList<>();
        for (List<PaymentPoint> group : groups.values()) {
            List<PaymentPoint> sorted = group.stream().sorted(Comparator.comparing(PaymentPoint::date)).toList();
            analyse(sorted, today).ifPresent(result::add);
        }
        result.sort(Comparator.comparing(Subscription::monthlyCost).reversed());
        return result;
    }

    private Optional<Subscription> analyse(List<PaymentPoint> payments, LocalDate today) {
        if (payments.size() < MIN_OCCURRENCES) {
            return Optional.empty();
        }
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < payments.size(); i++) {
            intervals.add(ChronoUnit.DAYS.between(payments.get(i - 1).date(), payments.get(i).date()));
        }
        Optional<Cadence> cadence = cadenceOf(intervals);
        if (cadence.isEmpty() || !amountsSimilar(payments)) {
            return Optional.empty();
        }
        PaymentPoint last = payments.get(payments.size() - 1);
        LocalDate next = last.date().plusDays(cadence.get().typicalDays());
        boolean active = !today.isAfter(next.plusDays(cadence.get().typicalDays() / 2));
        if (!active) {
            return Optional.empty();
        }
        BigDecimal previous = payments.get(payments.size() - 2).amount();
        boolean increased = last.amount().compareTo(previous.multiply(PRICE_INCREASE_THRESHOLD)) >= 0;
        return Optional.of(new Subscription(last.merchant(), last.currency(), cadence.get(), last.amount(), previous,
                increased, monthlyCost(last.amount(), cadence.get()), last.date(), next, payments.size(),
                payments.stream().map(PaymentPoint::transactionId).toList()));
    }

    /** The cadence whose range contains all intervals (one irregular interval is tolerated). */
    private static Optional<Cadence> cadenceOf(List<Long> intervals) {
        for (Cadence cadence : Cadence.values()) {
            long irregular = intervals.stream().filter(d -> !cadence.accepts(d)).count();
            if (irregular == 0 || (irregular == 1 && intervals.size() >= 3)) {
                return Optional.of(cadence);
            }
        }
        return Optional.empty();
    }

    private static boolean amountsSimilar(List<PaymentPoint> payments) {
        List<BigDecimal> amounts = payments.stream().map(PaymentPoint::amount).sorted().toList();
        BigDecimal median = amounts.get(amounts.size() / 2);
        BigDecimal tolerance = median.multiply(AMOUNT_TOLERANCE);
        return amounts.stream().allMatch(a -> a.subtract(median).abs().compareTo(tolerance) <= 0);
    }

    private static BigDecimal monthlyCost(BigDecimal amount, Cadence cadence) {
        return switch (cadence) {
            case WEEKLY -> amount.multiply(WEEKS_PER_MONTH).setScale(2, RoundingMode.HALF_UP);
            case MONTHLY -> amount.setScale(2, RoundingMode.HALF_UP);
            case YEARLY -> amount.divide(MONTHS_PER_YEAR, 2, RoundingMode.HALF_UP);
        };
    }
}
