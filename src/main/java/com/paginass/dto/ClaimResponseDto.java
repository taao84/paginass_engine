package com.paginass.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClaimResponseDto(
        UUID id,
        UUID businessId,
        String businessName,
        String businessSlug,
        UUID claimantId,
        String claimantName,
        String claimantEmail,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt
) {}
