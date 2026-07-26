package com.featureforge.dto;

public record EvaluateFlagResponse(
        String flagKey,
        String targetingKey,
        boolean enabled,
        String reason
) {
}
