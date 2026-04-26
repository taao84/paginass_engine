package com.paginass.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record BusinessProfileEditDto(
        UUID id,
        UUID businessProfileId,
        UUID editedById,
        String editedByName,
        OffsetDateTime editedAt,
        Map<String, Object> snapshot
) {}
