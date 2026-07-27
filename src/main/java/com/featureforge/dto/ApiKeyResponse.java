package com.featureforge.dto;

import com.featureforge.domain.ApiKey;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id,
        String name,
        String keyPrefix,
        boolean revoked,
        Instant createdAt,
        Instant lastUsedAt
) {
    public static ApiKeyResponse fromEntity(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(), apiKey.getName(), apiKey.getKeyPrefix(),
                apiKey.isRevoked(), apiKey.getCreatedAt(), apiKey.getLastUsedAt());
    }
}
