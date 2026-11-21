package com.pocketlens.assistant.service;

import static com.pocketlens.support.TestData.OTHER_USER_ID;
import static com.pocketlens.support.TestData.USER_ID;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pocketlens.common.error.RateLimitExceededException;
import com.pocketlens.support.TestData;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SlidingWindowRateLimiterTest {

    @Test
    void allowsTwentyRequestsPerMinutePerUser() {
        MutableClock clock = new MutableClock(TestData.NOW);
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(TestData.properties(), clock);
        for (int i = 0; i < 20; i++) {
            limiter.acquire(USER_ID);
        }
        assertThatThrownBy(() -> limiter.acquire(USER_ID)).isInstanceOf(RateLimitExceededException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(
                        ((RateLimitExceededException) e).retryAfterSeconds()).isPositive());
        assertThatCode(() -> limiter.acquire(OTHER_USER_ID)).doesNotThrowAnyException();

        clock.advance(Duration.ofSeconds(61));
        assertThatCode(() -> limiter.acquire(USER_ID)).doesNotThrowAnyException();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
