package com.featureforge.dto;

import com.featureforge.domain.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String role
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }
}
