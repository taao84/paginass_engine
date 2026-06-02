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
        String bannerUrl,
        List<String> tags,
        boolean isClaimed,
        boolean hasPendingClaim,
        boolean deliversToHome,
        Double latitude,
        Double longitude,
        long likeCount,
        boolean isOwner
) {}
