package com.paginass.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ContactMessageSummaryDto(
        UUID id,
        String subject,
        String replyEmail,
        String message,
        OffsetDateTime submittedAt,
        OffsetDateTime readAt
) {}
