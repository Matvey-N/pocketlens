package com.pocketlens.subscription.service;

/**
 * Billing period of a recurring payment with the accepted interval range in days.
 */
public enum Cadence {
    WEEKLY(7, 6, 8),
    MONTHLY(30, 26, 35),
    YEARLY(365, 355, 375);

    private final int typicalDays;
    private final int minDays;
    private final int maxDays;

    Cadence(int typicalDays, int minDays, int maxDays) {
        this.typicalDays = typicalDays;
        this.minDays = minDays;
        this.maxDays = maxDays;
    }

    public int typicalDays() {
        return typicalDays;
    }

    public boolean accepts(long days) {
        return days >= minDays && days <= maxDays;
    }
}
