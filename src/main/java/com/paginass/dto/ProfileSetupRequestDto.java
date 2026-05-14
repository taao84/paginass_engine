package com.paginass.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ProfileSetupRequestDto(
        @NotNull AccountType accountType,
        String fullName,
        String businessName,
        Boolean isBrickAndMortar,
        String address,
        String description,
        String phones,
        String contactHours,
        List<String> services,
        Boolean deliversToHome,
        Double latitude,
        Double longitude,
        @Pattern(regexp = "^\\d{10}$", message = "El número de WhatsApp debe tener exactamente 10 dígitos")
        String whatsapp,
        String email,
        String website,
        @Pattern(regexp = "^(@[a-zA-Z0-9_.]{1,30})?$", message = "El usuario de Instagram debe tener el formato @nombre")
        String instagram
) {
    public enum AccountType { PERSON, BUSINESS }
}
