package com.paginass.dto;

import java.util.List;

public record AppNewsPageDto(
        List<AppNewsDto> content,
        int page,
        int totalPages,
        long totalElements
) {}
