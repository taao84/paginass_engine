package com.paginass.dto;

import jakarta.validation.constraints.Min;

public record PhotoContestWinnerUpdateRequestDto(
        @Min(1) Integer winnerRank
) {}
