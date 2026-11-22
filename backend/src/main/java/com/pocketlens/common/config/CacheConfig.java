package com.pocketlens.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * Cache names. Redis is used in production ({@code CACHE_TYPE=redis}, TTL {@code spring.cache.redis.time-to-live}),
 * an in-memory cache locally and in tests. Cached values are serialisable records.
 */
@Configuration
public class CacheConfig {

    /** Dashboard read model per user and month. Evicted on every change of the user's data. */
    public static final String DASHBOARD_CACHE = "dashboard";
}
