package com.paginass.dto;

import java.util.UUID;

public record AuthResponseDto(
        UUID userId,
        String message
) {}
