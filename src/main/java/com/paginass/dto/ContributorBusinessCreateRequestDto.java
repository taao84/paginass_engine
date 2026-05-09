package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ContributorBusinessCreateRequestDto(
        @NotBlank String name,
        @NotBlank String bizType,
        String description,
        String phones,
        String whatsapp,
        String contactHours,
        String address,
        List<String> services,
        Boolean deliversToHome,
        Double latitude,
        Double longitude
) {}
