package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UpdateUserProfileRequestDto(
        @NotBlank String fullName,
        LocalDate dateOfBirth,
        String phone
) {}
