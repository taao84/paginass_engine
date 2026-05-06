package com.paginass.dto;

import java.util.UUID;

public record BusinessMapMarkerDto(
        UUID id,
        String name,
        String slug,
        double latitude,
        double longitude,
        String bizType
) {}
