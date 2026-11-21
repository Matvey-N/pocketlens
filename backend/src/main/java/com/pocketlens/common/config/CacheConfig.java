package com.pocketlens.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocketlens.dashboard.service.DashboardView;
import java.time.Duration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Cache names and the Redis serialisation of cached values.
 * When {@code spring.cache.type=simple} (local development, tests) an in-memory cache is used instead.
 */
@Configuration
public class CacheConfig {

    /** Dashboard read model per user and month. Evicted on every change of the user's data. */
    public static final String DASHBOARD_CACHE = "dashboard";

    private static final Duration DASHBOARD_TTL = Duration.ofMinutes(10);

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheCustomizer(ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<DashboardView> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, DashboardView.class);
        RedisCacheConfiguration dashboardConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DASHBOARD_TTL)
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return builder -> builder.withCacheConfiguration(DASHBOARD_CACHE, dashboardConfig);
    }
}
