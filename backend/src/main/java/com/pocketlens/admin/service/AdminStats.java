package com.pocketlens.admin.service;

/**
 * Service usage for administrators (US-7.2). Contains no personal data.
 */
public record AdminStats(long users, long transactions, long assistantAnswersLast30Days,
                         long aiCallsLast30Days, long inputTokensLast30Days, long outputTokensLast30Days,
                         boolean aiEnabled) {
}
