package com.pocketlens.importing.service.format;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row of a statement after parsing and normalisation (negative amount = expense).
 */
public record ParsedTransaction(LocalDate date, BigDecimal amount, String currency, String description) {
}
