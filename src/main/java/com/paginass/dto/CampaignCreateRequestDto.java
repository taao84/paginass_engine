package com.paginass.dto;

import com.paginass.entity.CampaignType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CampaignCreateRequestDto(
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull CampaignType campaignType,
        LocalDateTime startDatetime,
        @NotNull LocalDateTime endDatetime,
        String rules,
        String conditions,
        String awards
) {}
