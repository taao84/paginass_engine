package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

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
        Double longitude,
        String email,
        String website,
        @Pattern(regexp = "^(@[a-zA-Z0-9_.]{1,30})?$", message = "El usuario de Instagram debe tener el formato @nombre")
        String instagram
) {}
