package com.paginass.dto;

import java.util.List;

public record AdvertisementAdminPageDto(
        List<AdvertisementSummaryDto> content,
        int page,
        int totalPages,
        long totalElements
) {}
