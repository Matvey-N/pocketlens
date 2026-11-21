package com.pocketlens.subscription.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Minimal view of an expense used for subscription detection (amount is positive).
 */
public record PaymentPoint(UUID transactionId, String merchant, String currency, LocalDate date, BigDecimal amount) {
}
