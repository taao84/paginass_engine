package com.paginass.dto;

import com.paginass.entity.AdCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdvertisementSummaryDto(
        UUID id,
        AdCategory category,
        String title,
        BigDecimal price,
        Instant eventDate,
        List<String> tags,
        String firstImageUrl,
        Instant expiresAt,
        Instant createdAt,
        String ownerName,
        UUID businessId,
        String businessName,
        String businessSlug
) {}
