package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record BusinessProfileUpdateRequestDto(
        @NotBlank String name,
        String description,
        String phones,
        String contactHours,
        String address,
        List<String> services,
        Boolean deliversToHome,
        String bizType,
        Double latitude,
        Double longitude,
        @Pattern(regexp = "^\\d{10}$", message = "El número de WhatsApp debe tener exactamente 10 dígitos")
        String whatsapp
) {}
