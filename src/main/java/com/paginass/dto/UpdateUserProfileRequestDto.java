package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserProfileRequestDto(@NotBlank String fullName) {}
