package com.featureforge.event;

import com.featureforge.dto.FeatureFlagResponse;

/**
 * Broadcast to /topic/projects/{projectId}/flags whenever a flag is
 * created, updated, or deleted. The Angular dashboard subscribes per-project
 * and patches its local flag list on receipt instead of re-fetching.
 */
public record FlagChangeEvent(
        FlagChangeType type,
        FeatureFlagResponse flag
) {
    public enum FlagChangeType {
        CREATED,
        UPDATED,
        DELETED
    }
}
