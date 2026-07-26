package com.featureforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 255)
        String name,

        @NotBlank(message = "Key is required")
        @Pattern(regexp = "^[a-z0-9-]{2,100}$", message = "Key must be lowercase letters, numbers, and hyphens only")
        String key
) {
}
