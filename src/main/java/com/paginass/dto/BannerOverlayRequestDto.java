package com.paginass.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BannerOverlayRequestDto(
        @NotBlank
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "overlayColor must be a valid hex color (e.g. #1a2b3c)")
        String overlayColor,

        @DecimalMin(value = "0.0", message = "overlayOpacity must be at least 0.0")
        @DecimalMax(value = "1.0", message = "overlayOpacity must be at most 1.0")
        double overlayOpacity,

        @NotBlank
        @Size(max = 50, message = "overlayText must not exceed 50 characters")
        String overlayText,

        boolean overlayBorder,

        @Min(value = 0, message = "overlaySpacing must be at least 0")
        @Max(value = 400, message = "overlaySpacing must be at most 400")
        int overlaySpacing,

        @Min(value = 0, message = "overlayY must be at least 0")
        @Max(value = 240, message = "overlayY must be at most 240")
        int overlayY
) {}
