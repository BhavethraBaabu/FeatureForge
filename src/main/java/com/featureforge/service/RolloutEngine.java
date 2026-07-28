package com.featureforge.service;

import com.featureforge.domain.FeatureFlag;
import com.featureforge.domain.FlagOverride;
import com.featureforge.dto.EvaluateFlagResponse;
import com.featureforge.dto.FlagCacheEntry;
import com.featureforge.repository.FlagOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Deterministic flag evaluation.
 *
 * Order of precedence:
 *   1. Master kill switch (flag.enabled == false) -> always false.
 *   2. Explicit per-targeting-key override -> takes precedence over rollout math.
 *   3. Percentage rollout -> consistent hash bucketing so the same targetingKey
 *      always lands in the same bucket for a given flag ("sticky" rollout).
 *
 * Bucketing uses String.hashCode(), which the JLS guarantees is stable across
 * JVM versions/vendors for a given string.
 *
 * Two public overloads share the same bucketing/override logic: one takes the
 * JPA entity (dashboard's "test this flag" evaluate endpoint), the other takes
 * FlagCacheEntry (SDK path, reading through Redis via CachedFlagLookupService).
 * Overrides are never cached — they're looked up live in both cases — so a
 * newly-set override takes effect immediately even if the flag definition
 * itself is still served from cache.
 */
@Service
@RequiredArgsConstructor
public class RolloutEngine {

    private static final int BUCKET_SPACE = 100;

    private final FlagOverrideRepository flagOverrideRepository;

    public EvaluateFlagResponse evaluate(FeatureFlag flag, String targetingKey) {
        return evaluate(flag.getId(), flag.getKey(), flag.isEnabled(), flag.getRolloutPercentage(), targetingKey);
    }

    public EvaluateFlagResponse evaluate(FlagCacheEntry flag, String targetingKey) {
        return evaluate(flag.id(), flag.key(), flag.enabled(), flag.rolloutPercentage(), targetingKey);
    }

    private EvaluateFlagResponse evaluate(UUID flagId, String flagKey, boolean enabled,
                                          short rolloutPercentage, String targetingKey) {
        if (!enabled) {
            return new EvaluateFlagResponse(flagKey, targetingKey, false, "FLAG_DISABLED");
        }

        Optional<FlagOverride> override = flagOverrideRepository.findByFlagIdAndTargetingKey(flagId, targetingKey);

        if (override.isPresent()) {
            boolean value = override.get().isEnabled();
            return new EvaluateFlagResponse(flagKey, targetingKey, value, "OVERRIDE");
        }

        int bucket = bucketFor(flagKey, targetingKey);
        boolean inRollout = bucket < rolloutPercentage;

        return new EvaluateFlagResponse(
                flagKey, targetingKey, inRollout,
                inRollout ? "ROLLOUT_MATCH" : "ROLLOUT_MISS");
    }

    private int bucketFor(String flagKey, String targetingKey) {
        String composite = flagKey + ":" + targetingKey;
        return Math.floorMod(composite.hashCode(), BUCKET_SPACE);
    }
}
