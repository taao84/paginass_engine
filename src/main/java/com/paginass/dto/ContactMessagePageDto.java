package com.paginass.dto;

import java.util.List;

public record ContactMessagePageDto(
        List<ContactMessageSummaryDto> content,
        int page,
        int totalPages,
        long totalElements
) {}
