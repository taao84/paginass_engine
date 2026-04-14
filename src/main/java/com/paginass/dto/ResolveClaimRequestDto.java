package com.paginass.dto;

import jakarta.validation.constraints.NotNull;

public record ResolveClaimRequestDto(
        @NotNull Boolean approve
) {}
