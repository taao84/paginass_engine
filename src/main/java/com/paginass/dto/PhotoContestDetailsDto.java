package com.paginass.dto;

import java.util.UUID;

public record PhotoContestDetailsDto(
        UUID id,
        String rules,
        String conditions,
        String awards
) {}
