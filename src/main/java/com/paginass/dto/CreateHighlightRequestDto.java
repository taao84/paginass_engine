package com.paginass.dto;

import com.paginass.entity.HighlightType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateHighlightRequestDto(
        @NotNull HighlightType type,
        @NotNull UUID referenceId,
        @NotNull HighlightLifetime lifetime
) {}
