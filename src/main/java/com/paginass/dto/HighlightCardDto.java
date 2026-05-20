package com.paginass.dto;

import com.paginass.entity.HighlightType;

import java.util.UUID;

public record HighlightCardDto(
        UUID id,
        HighlightType type,
        String title,
        String thumbnailUrl,
        String linkPath,
        String badge,
        String description
) {}
