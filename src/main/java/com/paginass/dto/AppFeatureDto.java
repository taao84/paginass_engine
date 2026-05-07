package com.paginass.dto;

public record AppFeatureDto(
        String featureKey,
        String displayName,
        boolean enabled
) {}
