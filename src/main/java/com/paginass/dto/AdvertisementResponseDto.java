package com.paginass.dto;

import com.paginass.entity.AdCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdvertisementResponseDto(
        UUID id,
        AdCategory category,
        String title,
        String content,
        BigDecimal price,
        Instant eventDate,
        List<String> tags,
        List<String> imageUrls,
        String ownerName,
        UUID ownerId,
        UUID businessId,
        String businessName,
        String businessSlug,
        Instant createdAt,
        Instant expiresAt
) {}
