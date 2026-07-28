package com.featureforge.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Server-side record of an SDK key. Only hashedKey is stored — the raw
 * secret is shown to the caller exactly once, at creation time, and is
 * unrecoverable after that (same model as GitHub PATs / Stripe API keys).
 */
@Entity
@Table(name = "api_keys", uniqueConstraints = {
        @UniqueConstraint(name = "uq_api_key_prefix", columnNames = {"key_prefix"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String name;

    /** First 12 chars of the raw key, used for fast indexed lookup and shown in listings. */
    @Column(name = "key_prefix", nullable = false)
    private String keyPrefix;

    /** BCrypt hash of the full raw key. */
    @Column(name = "hashed_key", nullable = false)
    private String hashedKey;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
