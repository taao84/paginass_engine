package com.paginass.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FeaturedProductDto(
        UUID id,
        String title,
        String description,
        BigDecimal price,
        String imageUrl,
        int displayOrder
) {}
