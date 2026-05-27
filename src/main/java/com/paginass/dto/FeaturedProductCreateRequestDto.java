package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record FeaturedProductCreateRequestDto(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 1000) String description,
        BigDecimal price
) {}
