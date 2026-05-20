package com.paginass.dto;

import com.paginass.entity.HighlightType;

import java.time.Instant;
import java.util.UUID;

public record HighlightSummaryDto(
        UUID id,
        HighlightType type,
        String title,
        String badge,
        Instant expiresAt,
        boolean enabled
) {}
