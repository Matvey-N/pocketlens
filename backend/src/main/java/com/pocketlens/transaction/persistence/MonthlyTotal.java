package com.pocketlens.transaction.persistence;

import java.math.BigDecimal;

/**
 * Expenses and income of one calendar month in one currency (transfers excluded).
 */
public record MonthlyTotal(int year, int month, String currency, BigDecimal expenses, BigDecimal income) {
}
