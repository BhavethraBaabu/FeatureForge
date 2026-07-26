package com.featureforge.dto;

import com.featureforge.domain.Project;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID organizationId,
        String name,
        String key,
        Instant createdAt
) {
    public static ProjectResponse fromEntity(Project project) {
        return new ProjectResponse(
                project.getId(), project.getOrganizationId(), project.getName(),
                project.getKey(), project.getCreatedAt());
    }
}
