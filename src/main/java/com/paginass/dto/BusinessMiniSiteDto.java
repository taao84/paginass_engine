package com.paginass.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BusinessMiniSiteDto(
        UUID id,
        String name,
        String description,
        String bizType,
        Map<String, String> contactInfo,
        List<String> imageUrls,
        List<String> tags,
        boolean miniSiteEnabled,
        boolean isClaimed,
        boolean hasPendingClaim,
        boolean deliversToHome,
        Double latitude,
        Double longitude
) {}
