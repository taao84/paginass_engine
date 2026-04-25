package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfileSetupRequestDto(
        @NotNull AccountType accountType,
        String fullName,
        String businessName,
        Boolean isBrickAndMortar,
        String address,
        String description,
        String phones,
        String contactHours,
        String services,
        Boolean deliversToHome
) {
    public enum AccountType { PERSON, BUSINESS }
}
