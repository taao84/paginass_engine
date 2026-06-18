package com.paginass.dto;

import java.util.List;

public record AdminBusinessPageDto(
        List<AdminBusinessSummaryDto> content,
        int page,
        int totalPages,
        long totalElements
) {}
