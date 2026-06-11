package com.paginass.dto;

import java.util.UUID;

public record PhotoContestLikeDto(
        UUID entryId,
        long likeCount,
        boolean hasLiked
) {}
