package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAppNewsRequestDto(
        @NotBlank String title,
        @NotBlank String description
) {}
