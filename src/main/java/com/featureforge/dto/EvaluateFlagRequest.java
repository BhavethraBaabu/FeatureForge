package com.featureforge.dto;

import jakarta.validation.constraints.NotBlank;

public record EvaluateFlagRequest(

        @NotBlank(message = "Targeting key is required")
        String targetingKey
) {
}
