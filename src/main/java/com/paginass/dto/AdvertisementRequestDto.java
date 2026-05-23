package com.paginass.dto;

import com.paginass.entity.AdCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdvertisementRequestDto(
        @NotNull AdCategory category,
        @NotBlank String title,
        @NotBlank String content,
        BigDecimal price,
        Instant eventDate,
        List<String> tags,
        UUID businessId
) {}
