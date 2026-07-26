package com.featureforge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateFeatureFlagRequest(

        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description,

        Boolean enabled,

        @Min(0) @Max(100)
        Short rolloutPercentage
) {
}
