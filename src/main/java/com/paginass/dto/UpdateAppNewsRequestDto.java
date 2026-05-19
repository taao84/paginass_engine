package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAppNewsRequestDto(
        @NotBlank String title,
        @NotBlank String description,
        boolean removeImage
) {}
