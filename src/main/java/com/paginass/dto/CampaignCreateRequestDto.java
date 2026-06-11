package com.paginass.dto;

import com.paginass.entity.CampaignType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CampaignCreateRequestDto(
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull CampaignType campaignType,
        @NotNull OffsetDateTime endDatetime,
        String rules,
        String conditions,
        String awards
) {}
