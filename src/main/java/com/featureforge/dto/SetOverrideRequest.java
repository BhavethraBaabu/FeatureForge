package com.featureforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetOverrideRequest(

        @NotBlank(message = "Targeting key is required")
        String targetingKey,

        @NotNull(message = "Enabled is required")
        Boolean enabled
) {
}
