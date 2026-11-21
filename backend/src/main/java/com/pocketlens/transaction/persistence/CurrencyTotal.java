package com.pocketlens.transaction.persistence;

import java.math.BigDecimal;

/**
 * Sum of amounts in one currency.
 */
public record CurrencyTotal(String currency, BigDecimal total, long count) {
}
