package com.featureforge.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Returned ONLY from the create endpoint. `rawKey` is never persisted and
 * never retrievable again after this response — same model as GitHub PATs
 * and Stripe API keys.
 */
public record ApiKeyCreatedResponse(
        UUID id,
        String name,
        String keyPrefix,
        String rawKey,
        Instant createdAt
) {
}
