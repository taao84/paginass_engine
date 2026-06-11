package com.paginass.dto;

import java.util.UUID;

public record CampaignBusinessDto(
        UUID businessId,
        String name,
        String slug,
        String thumbnailUrl
) {}
