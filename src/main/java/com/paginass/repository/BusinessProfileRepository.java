package com.paginass.repository;

import com.paginass.entity.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, UUID> {

    List<BusinessProfile> findByOwnerIdAndIsClaimedTrueAndIsActiveTrue(UUID ownerId);

    Optional<BusinessProfile> findByIdAndOwnerIdAndIsClaimedTrue(UUID id, UUID ownerId);

    boolean existsByOwnerIdAndIsClaimedTrue(UUID ownerId);

    Optional<BusinessProfile> findBySlugAndIsActiveTrue(String slug);

    // Column order in SELECT must stay in sync with BusinessProfileService.mapRow()
    // [0] id, [1] name, [2] slug, [3] biz_type, [4] description, [5] tags, [6] contact_info, [7] score, [8] mini_site_enabled, [9] is_claimed, [10] delivers_to_home
    @Query(value = """
            SELECT b.id::text,
                   b.name,
                   b.slug,
                   b.biz_type::text,
                   b.description,
                   b.tags::text,
                   b.contact_info::text,
                   ts_rank(b.search_vector, websearch_to_tsquery('spanish', :query)) AS score,
                   b.mini_site_enabled,
                   b.is_claimed,
                   b.delivers_to_home
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

    // Column order: [0] name, [1] slug, [2] biz_type
    @Query(value = """
            SELECT b.name,
                   b.slug,
                   b.biz_type::text
            FROM   business_profiles b
            WHERE  b.is_active = true
            ORDER  BY b.biz_type ASC, b.name ASC
            LIMIT  :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findAllForDirectory(@Param("limit") int limit, @Param("offset") int offset);

    @Query(value = """
            SELECT count(*)
            FROM   business_profiles b
            WHERE  b.is_active = true
            """, nativeQuery = true)
    long countAllForDirectory();
}
