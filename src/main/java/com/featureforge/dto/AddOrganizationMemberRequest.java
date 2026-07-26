package com.featureforge.dto;

import com.featureforge.domain.OrgRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddOrganizationMemberRequest(

        @NotNull(message = "userId is required")
        UUID userId,

        @NotNull(message = "role is required")
        OrgRole role
) {
}
