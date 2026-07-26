package com.featureforge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFeatureFlagRequest(

        @NotBlank(message = "Key is required")
        @Pattern(regexp = "^[a-z0-9_-]{2,150}$", message = "Key must be lowercase letters, numbers, hyphens, underscores only")
        String key,

        @NotBlank(message = "Name is required")
        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description,

        boolean enabled,

        @Min(0) @Max(100)
        short rolloutPercentage
) {
}
