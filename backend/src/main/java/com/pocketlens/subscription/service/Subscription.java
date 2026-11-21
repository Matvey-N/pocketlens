package com.pocketlens.subscription.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A detected recurring payment (US-5.1).
 *
 * @param previousAmount amount of the payment before the last one; differs from {@code lastAmount} on a price change
 * @param priceIncreased the last payment is at least 1 % more expensive than the previous one (US-5.2)
 * @param monthlyCost    cost normalised to one month
 */
public record Subscription(String merchant, String currency, Cadence cadence, BigDecimal lastAmount,
                           BigDecimal previousAmount, boolean priceIncreased, BigDecimal monthlyCost,
                           LocalDate lastPaymentDate, LocalDate nextExpectedDate, int occurrences,
                           List<UUID> transactionIds) {
}
