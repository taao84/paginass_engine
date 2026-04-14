package com.paginass.dto;

import java.util.List;

public record DirectoryPageDto(
        List<DirectoryEntryDto> content,
        int page,
        int totalPages,
        long totalElements
) {}
