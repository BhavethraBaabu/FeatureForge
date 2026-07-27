package com.featureforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 255)
        String name
) {
}
