package com.pocketlens.assistant.service;

import com.pocketlens.common.config.AppProperties;
import com.pocketlens.common.error.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Per-user sliding-window limit of assistant requests (NFR-S4: 20 per minute). In-memory, i.e. per instance.
 */
@Component
public class SlidingWindowRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final int limit;
    private final Clock clock;
    private final Map<UUID, Deque<Instant>> requests = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(AppProperties properties, Clock clock) {
        this.limit = properties.assistant().requestsPerMinute();
        this.clock = clock;
    }

    /** Registers a request or throws {@link RateLimitExceededException} with the seconds to wait. */
    public void acquire(UUID userId) {
        Instant now = clock.instant();
        Deque<Instant> window = requests.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && !window.peekFirst().isAfter(now.minus(WINDOW))) {
                window.pollFirst();
            }
            if (window.size() >= limit) {
                long wait = Duration.between(now, window.peekFirst().plus(WINDOW)).toSeconds() + 1;
                throw new RateLimitExceededException(wait);
            }
            window.addLast(now);
        }
    }
}
