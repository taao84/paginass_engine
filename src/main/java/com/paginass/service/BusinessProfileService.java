package com.paginass.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paginass.dto.BusinessProfileSummaryDto;
import com.paginass.dto.SearchResponseDto;
import com.paginass.repository.BusinessProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessProfileService {

    private static final int PAGE_SIZE = 10;

    private final BusinessProfileRepository businessProfileRepository;
    private final ObjectMapper objectMapper;

    public SearchResponseDto search(String query, int page) {
        int offset = page * PAGE_SIZE;

        List<Object[]> rows = businessProfileRepository.searchBusinessProfiles(query, PAGE_SIZE, offset);
        long total = businessProfileRepository.countSearchResults(query);

        List<BusinessProfileSummaryDto> results = rows.stream()
                .map(this::mapRow)
                .toList();

        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        return new SearchResponseDto(results, page, totalPages, total);
    }

    // Column positions must match the SELECT order in BusinessProfileRepository.searchBusinessProfiles:
    // [0] id, [1] name, [2] slug, [3] biz_type, [4] description, [5] tags, [6] contact_info, [7] score
    private BusinessProfileSummaryDto mapRow(Object[] row) {
        try {
            UUID id = UUID.fromString((String) row[0]);
            String name        = (String) row[1];
            String slug        = (String) row[2];
            String bizType     = (String) row[3];
            String description = (String) row[4];

            List<String> tags = row[5] != null
                    ? objectMapper.readValue((String) row[5], new TypeReference<>() {})
                    : Collections.emptyList();

            Map<String, String> contactInfo = row[6] != null
                    ? objectMapper.readValue((String) row[6], new TypeReference<>() {})
                    : Collections.emptyMap();

            double score = row[7] != null ? ((Number) row[7]).doubleValue() : 0.0;

            return new BusinessProfileSummaryDto(id, name, slug, bizType, description, tags, contactInfo, score);
        } catch (Exception e) {
            throw new RuntimeException("Failed to map search result row", e);
        }
    }
}
