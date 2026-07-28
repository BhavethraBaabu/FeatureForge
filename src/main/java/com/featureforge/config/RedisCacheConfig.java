package com.featureforge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Caches flag *definitions* (key, enabled, rolloutPercentage) for the SDK
 * evaluation path only — never per-user evaluation results, since override
 * lookups (RolloutEngine -> FlagOverrideRepository) have to stay live or a
 * targeted override would appear to "not take effect" until the cache expired.
 *
 * TTL is deliberately short: a flag toggle in the dashboard should reach SDK
 * callers quickly. Explicit @CacheEvict on every mutation (see
 * CachedFlagLookupService) handles the common case instantly; the TTL is
 * just a safety net for any write path that doesn't go through that evict.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Value("${featureforge.cache.flag-ttl-seconds:60}")
    private long flagTtlSeconds;

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(flagTtlSeconds))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper())));

        return builder -> builder.cacheDefaults(config);
    }

    /**
     * Redis needs Instant/UUID-aware Jackson, not the bare default mapper —
     * without JavaTimeModule, any cached type carrying an Instant blows up
     * on serialization.
     */
    private ObjectMapper redisObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }
}
