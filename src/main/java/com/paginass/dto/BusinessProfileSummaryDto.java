package com.paginass.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BusinessProfileSummaryDto(
        UUID id,
        String name,
        String slug,
        String bizType,
        String description,
        List<String> tags,
        Map<String, String> contactInfo,
        double score,
        boolean miniSiteEnabled,
        boolean isClaimed,
        boolean deliversToHome
) {}
