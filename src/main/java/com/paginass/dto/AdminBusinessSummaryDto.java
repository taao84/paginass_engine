package com.paginass.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminBusinessSummaryDto(
        UUID id,
        String name,
        String slug,
        String bizType,
        boolean isClaimed,
        boolean isActive,
        OffsetDateTime createdAt
) {}
