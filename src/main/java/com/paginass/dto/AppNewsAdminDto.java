package com.paginass.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppNewsAdminDto(
        UUID id,
        String title,
        String description,
        String imageUrl,
        OffsetDateTime createdAt,
        String createdByName
) {}
