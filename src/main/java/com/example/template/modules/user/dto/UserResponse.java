package com.example.template.modules.user.dto;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        boolean enabled,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
