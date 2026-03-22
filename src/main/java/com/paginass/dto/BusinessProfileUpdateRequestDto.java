package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;

public record BusinessProfileUpdateRequestDto(
        @NotBlank String name,
        String description,
        String phones,
        String contactHours,
        String address,
        String services
) {}
