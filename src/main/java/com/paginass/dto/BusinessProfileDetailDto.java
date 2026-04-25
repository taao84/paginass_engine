package com.paginass.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BusinessProfileDetailDto(
        UUID id,
        String name,
        String slug,
        String bizType,
        String description,
        Map<String, String> contactInfo,
        List<String> tags,
        List<String> imageUrls,
        boolean miniSiteEnabled,
        boolean isActive,
        boolean isClaimed,
        UUID createdBy,
        boolean deliversToHome
) {}
