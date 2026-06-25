package com.example.template.modules.user.dto;

import com.example.template.modules.role.entity.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "must contain only letters, numbers, dots, underscores, or hyphens")
        String username,

        @Email
        @Size(max = 255)
        String email,

        @Size(min = 8, max = 72)
        String password,

        @Size(min = 1, max = 150)
        String fullName,

        Boolean enabled,

        Set<RoleName> roles
) {
}
