package com.paginass.repository;

import com.paginass.entity.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, UUID> {

    List<BusinessProfile> findByOwnerIdAndIsActiveTrue(UUID ownerId);

    Optional<BusinessProfile> findByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<BusinessProfile> findBySlugAndIsActiveTrue(String slug);

    // Column order in SELECT must stay in sync with BusinessProfileService.mapRow()
    // [0] id, [1] name, [2] slug, [3] biz_type, [4] description, [5] tags, [6] contact_info, [7] score, [8] mini_site_enabled
    @Query(value = """
            SELECT b.id::text,
                   b.name,
                   b.slug,
                   b.biz_type::text,
                   b.description,
                   b.tags::text,
                   b.contact_info::text,
                   ts_rank(b.search_vector, websearch_to_tsquery('spanish', :query)) AS score,
                   b.mini_site_enabled
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.search_vector @@ websearch_to_tsquery('spanish', :query)
            ORDER  BY score DESC
            LIMIT  :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> searchBusinessProfiles(
            @Param("query") String query,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query(value = """
            SELECT count(*)
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.search_vector @@ websearch_to_tsquery('spanish', :query)
            """, nativeQuery = true)
    long countSearchResults(@Param("query") String query);
}
