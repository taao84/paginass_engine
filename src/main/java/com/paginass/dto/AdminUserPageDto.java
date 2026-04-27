package com.paginass.dto;

import java.util.List;

public record AdminUserPageDto(
        List<AdminUserDto> content,
        int page,
        int totalPages,
        long totalElements
) {}
