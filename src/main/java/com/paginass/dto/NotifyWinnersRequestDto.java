package com.paginass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record NotifyWinnersRequestDto(
        @NotBlank String subject,
        @NotBlank String message,
        @NotEmpty List<UUID> entryIds
) {}
