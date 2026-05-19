package com.paginass.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppNewsDto(
        UUID id,
        String title,
        String description,
        String imageUrl,
        OffsetDateTime createdAt
) {}
