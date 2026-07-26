package com.featureforge.dto;

import com.featureforge.domain.FeatureFlag;

import java.time.Instant;
import java.util.UUID;

public record FeatureFlagResponse(
        UUID id,
        UUID projectId,
        String key,
        String name,
        String description,
        boolean enabled,
        short rolloutPercentage,
        Instant createdAt,
        Instant updatedAt
) {
    public static FeatureFlagResponse fromEntity(FeatureFlag flag) {
        return new FeatureFlagResponse(
                flag.getId(), flag.getProjectId(), flag.getKey(), flag.getName(),
                flag.getDescription(), flag.isEnabled(), flag.getRolloutPercentage(),
                flag.getCreatedAt(), flag.getUpdatedAt());
    }
}
