package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record BusinessProfileUpdateRequestDto(
        @NotBlank String name,
        String description,
        String phones,
        String contactHours,
        String address,
        List<String> services,
        Boolean deliversToHome,
        String bizType
) {}
