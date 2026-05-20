package com.paginass.dto;

import java.time.Instant;
import java.util.UUID;

public record JobRunDto(
        UUID id,
        String jobName,
        String triggeredBy,
        Instant startedAt,
        Instant finishedAt,
        String status,
        String message
) {}
