package com.paginass.dto;

import com.paginass.entity.CampaignStatus;
import jakarta.validation.constraints.NotNull;

public record CampaignStatusUpdateRequestDto(
        @NotNull CampaignStatus status
) {}
