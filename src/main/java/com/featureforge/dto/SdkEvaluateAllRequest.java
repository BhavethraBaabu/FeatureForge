package com.featureforge.dto;

import jakarta.validation.constraints.NotBlank;

public record SdkEvaluateAllRequest(

        @NotBlank(message = "targetingKey is required")
        String targetingKey
) {
}
