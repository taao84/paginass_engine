package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdvertisementUpdateRequestDto(
        @NotBlank String title,
        @NotBlank String content,
        BigDecimal price,
        Instant eventDate,
        List<String> tags
) {}
