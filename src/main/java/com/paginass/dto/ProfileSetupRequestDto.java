package com.paginass.dto;

import jakarta.validation.constraints.NotNull;

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
        Boolean deliversToHome
) {
    public enum AccountType { PERSON, BUSINESS }
}
