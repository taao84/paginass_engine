package com.paginass.dto;

import com.paginass.entity.CampaignStatus;
import com.paginass.entity.CampaignType;

import java.util.List;
import java.util.UUID;

public record CampaignDto(
        UUID id,
        String name,
        String description,
        CampaignType campaignType,
        CampaignStatus status,
        String slug,
        String imageUrl,
        String startDatetimeDisplay,
        String startDatetimeInput,
        String endDatetimeDisplay,
        String endDatetimeInput,
        PhotoContestDetailsDto photoContestDetails,
        List<CampaignBusinessDto> linkedBusinesses
) {}
