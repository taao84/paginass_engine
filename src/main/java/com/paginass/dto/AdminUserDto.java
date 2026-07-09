package com.paginass.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserDto(
        UUID id,
        String email,
        String fullName,
        String role,
        boolean enabled,
        boolean emailVerified,
        String oauthProvider,
        String phone,
        LocalDate dateOfBirth,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
