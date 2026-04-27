package com.paginass.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserDto(
        UUID id,
        String email,
        String fullName,
        String role,
        boolean enabled,
        OffsetDateTime createdAt
) {}
