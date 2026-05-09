package com.paginass.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BannerHistoryEntryDto(UUID id, String url, OffsetDateTime uploadedAt) {}
