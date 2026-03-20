package com.paginass.dto;

import java.util.List;

public record SearchResponseDto(
        List<BusinessProfileSummaryDto> results,
        int page,
        int totalPages,
        long totalResults
) {}
