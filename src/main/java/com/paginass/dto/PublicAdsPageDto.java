package com.paginass.dto;

import java.util.List;

public record PublicAdsPageDto(
        List<AdvertisementSummaryDto> content,
        int page,
        int totalPages,
        boolean hasMore
) {}
