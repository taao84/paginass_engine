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
                   GREATEST(
                       ts_rank(b.search_vector, websearch_to_tsquery('spanish', :rawQuery)),
                       ts_rank(b.search_vector, to_tsquery('spanish', :prefixQuery))
                   ) AS score,
                   b.mini_site_enabled,
                   b.is_claimed,
                   b.delivers_to_home
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.search_vector @@ (
                       websearch_to_tsquery('spanish', :rawQuery) || to_tsquery('spanish', :prefixQuery)
                   )
            ORDER  BY score DESC
            LIMIT  :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> searchBusinessProfiles(
            @Param("rawQuery") String rawQuery,
            @Param("prefixQuery") String prefixQuery,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query(value = """
            SELECT count(*)
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.search_vector @@ (
                       websearch_to_tsquery('spanish', :rawQuery) || to_tsquery('spanish', :prefixQuery)
                   )
            """, nativeQuery = true)
    long countSearchResults(@Param("rawQuery") String rawQuery, @Param("prefixQuery") String prefixQuery);

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

    // Column order: [0] name, [1] slug, [2] biz_type
    @Query(value = """
            SELECT b.name,
                   b.slug,
                   b.biz_type::text
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  UPPER(LEFT(b.name, 1)) = UPPER(:letter)
            ORDER  BY b.name ASC
            LIMIT  :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findByFirstLetter(@Param("letter") String letter,
                                     @Param("limit") int limit,
                                     @Param("offset") int offset);

    @Query(value = """
            SELECT count(*)
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  UPPER(LEFT(b.name, 1)) = UPPER(:letter)
            """, nativeQuery = true)
    long countByFirstLetter(@Param("letter") String letter);

    // Column order: [0] name, [1] slug, [2] biz_type
    @Query(value = """
            SELECT b.name,
                   b.slug,
                   b.biz_type::text
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.name !~* '^[a-záéíóúñü]'
            ORDER  BY b.name ASC
            LIMIT  :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findByNonAlphaStart(@Param("limit") int limit, @Param("offset") int offset);

    @Query(value = """
            SELECT count(*)
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.name !~* '^[a-záéíóúñü]'
            """, nativeQuery = true)
    long countByNonAlphaStart();

    // Column order: [0] name, [1] slug, [2] biz_type
    @Query(value = """
            SELECT b.name,
                   b.slug,
                   b.biz_type::text
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.tags @> jsonb_build_array(:tag)
            ORDER  BY b.name ASC
            LIMIT  :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> findByTag(@Param("tag") String tag,
                              @Param("limit") int limit,
                              @Param("offset") int offset);

    @Query(value = """
            SELECT count(*)
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.tags @> jsonb_build_array(:tag)
            """, nativeQuery = true)
    long countByTag(@Param("tag") String tag);

    // Column order: [0] id, [1] name, [2] slug, [3] biz_type, [4] latitude, [5] longitude
    @Query(value = """
            SELECT b.id::text,
                   b.name,
                   b.slug,
                   b.biz_type::text,
                   b.latitude,
                   b.longitude
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.latitude  IS NOT NULL
              AND  b.longitude IS NOT NULL
            ORDER  BY b.name ASC
            """, nativeQuery = true)
    List<Object[]> findAllWithLocation();

    // Column order: [0] tag (text), [1] count (bigint)
    @Query(value = """
            SELECT t.tag, count(*) AS cnt
            FROM   business_profiles b,
                   jsonb_array_elements_text(b.tags) AS t(tag)
            WHERE  b.is_active = true
              AND  jsonb_array_length(b.tags) > 0
            GROUP  BY t.tag
            ORDER  BY cnt DESC, t.tag ASC
            """, nativeQuery = true)
    List<Object[]> findAllTagsWithCount();
}
