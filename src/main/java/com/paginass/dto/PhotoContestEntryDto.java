package com.paginass.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PhotoContestEntryDto(
        UUID id,
        UUID campaignId,
        String title,
        String description,
        String photoUrl,
        String authorName,
        UUID authorId,
        long likeCount,
        boolean hasLiked,
        boolean disabled,
        Integer winnerRank,
        boolean notified,
        OffsetDateTime createdAt
) {}
