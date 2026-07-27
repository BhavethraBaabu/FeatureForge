package com.featureforge.dto;

import jakarta.validation.constraints.NotBlank;

public record SdkEvaluateRequest(

        @NotBlank(message = "flagKey is required")
        String flagKey,

        @NotBlank(message = "targetingKey is required")
        String targetingKey
) {
}
