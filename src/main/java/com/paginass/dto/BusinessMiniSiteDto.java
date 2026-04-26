package com.paginass.dto;

import java.util.List;
import java.util.Map;

public record BusinessMiniSiteDto(
        String name,
        String description,
        Map<String, String> contactInfo,
        List<String> imageUrls,
        List<String> tags,
        boolean miniSiteEnabled,
        boolean isClaimed
) {}
