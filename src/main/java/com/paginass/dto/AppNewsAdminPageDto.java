package com.paginass.dto;

import java.util.List;

public record AppNewsAdminPageDto(
        List<AppNewsAdminDto> content,
        int page,
        int totalPages,
        long totalElements
) {}
