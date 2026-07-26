package com.featureforge.service;

import com.featureforge.domain.FeatureFlag;
import com.featureforge.domain.FlagOverride;
import com.featureforge.dto.EvaluateFlagResponse;
import com.featureforge.repository.FlagOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RolloutEngine {

    private static final int BUCKET_SPACE = 100;

    private final FlagOverrideRepository flagOverrideRepository;

    public EvaluateFlagResponse evaluate(FeatureFlag flag, String targetingKey) {
        if (!flag.isEnabled()) {
            return new EvaluateFlagResponse(flag.getKey(), targetingKey, false, "FLAG_DISABLED");
        }

        Optional<FlagOverride> override = flagOverrideRepository
                .findByFlagIdAndTargetingKey(flag.getId(), targetingKey);

        if (override.isPresent()) {
            boolean value = override.get().isEnabled();
            return new EvaluateFlagResponse(flag.getKey(), targetingKey, value, "OVERRIDE");
        }

        int bucket = bucketFor(flag.getKey(), targetingKey);
        boolean inRollout = bucket < flag.getRolloutPercentage();

        return new EvaluateFlagResponse(
                flag.getKey(), targetingKey, inRollout,
                inRollout ? "ROLLOUT_MATCH" : "ROLLOUT_MISS");
    }

    private int bucketFor(String flagKey, String targetingKey) {
        String composite = flagKey + ":" + targetingKey;
        return Math.floorMod(composite.hashCode(), BUCKET_SPACE);
    }
}
