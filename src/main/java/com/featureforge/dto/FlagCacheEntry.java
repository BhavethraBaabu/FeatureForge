package com.featureforge.dto;

import com.featureforge.domain.FeatureFlag;

import java.io.Serializable;
import java.util.UUID;

/**
 * What actually goes into Redis. Deliberately not the FeatureFlag JPA entity:
 * entities carry Hibernate proxy/lazy-loading baggage that doesn't survive
 * JSON round-tripping cleanly, and caching entities directly is a classic
 * way to leak stale/detached objects back into a transaction. This is a
 * flat, plain-Java snapshot of exactly the fields evaluation needs.
 */
public record FlagCacheEntry(
        UUID id,
        String key,
        boolean enabled,
        short rolloutPercentage
) implements Serializable {

    public static FlagCacheEntry fromEntity(FeatureFlag flag) {
        return new FlagCacheEntry(flag.getId(), flag.getKey(), flag.isEnabled(), flag.getRolloutPercentage());
    }
}
