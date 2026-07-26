package com.featureforge.dto;

import com.featureforge.domain.Organization;
import com.featureforge.domain.OrgRole;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        OrgRole yourRole,
        Instant createdAt
) {
    public static OrganizationResponse fromEntity(Organization org, OrgRole yourRole) {
        return new OrganizationResponse(org.getId(), org.getName(), org.getSlug(), yourRole, org.getCreatedAt());
    }
}
