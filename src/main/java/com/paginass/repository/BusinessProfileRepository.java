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

    @Query(value = """
            SELECT b.id::text                                                          AS id,
                   b.name                                                              AS name,
                   b.slug                                                              AS slug,
                   b.biz_type::text                                                    AS bizType,
                   b.description                                                       AS description,
                   b.tags::text                                                        AS tags,
                   b.contact_info::text                                                AS contactInfo,
                   GREATEST(
                       ts_rank(b.search_vector, websearch_to_tsquery('spanish', :rawQuery)),
                       ts_rank(b.search_vector, to_tsquery('spanish', :prefixQuery))
                   )                                                                   AS score,
                   b.is_claimed                                                        AS isClaimed,
                   b.delivers_to_home                                                  AS deliversToHome
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.search_vector @@ (
                       websearch_to_tsquery('spanish', :rawQuery) || to_tsquery('spanish', :prefixQuery)
                   )
            ORDER  BY score DESC
            LIMIT  :limit OFFSET :offset
            """, nativeQuery = true)
    List<BusinessProfileSearchProjection> searchBusinessProfiles(
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

    @Query(value = """
            SELECT b.id::text                                                          AS id,
                   b.name                                                              AS name,
                   b.slug                                                              AS slug,
                   b.biz_type::text                                                    AS bizType,
                   b.description                                                       AS description,
                   b.tags::text                                                        AS tags,
                   b.contact_info::text                                                AS contactInfo,
                   GREATEST(
                       ts_rank(b.search_vector,        websearch_to_tsquery('spanish', :rawQuery)),
                       ts_rank(b.search_vector_simple, to_tsquery('simple',            :prefixQuery))
                   )                                                                   AS score,
                   b.is_claimed                                                        AS isClaimed,
                   b.delivers_to_home                                                  AS deliversToHome
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  (
                       b.search_vector        @@ websearch_to_tsquery('spanish', :rawQuery)
                   OR  b.search_vector_simple @@ to_tsquery('simple',            :prefixQuery)
                   )
            ORDER  BY (CASE WHEN b.search_vector_simple @@ to_tsquery('simple', :prefixQuery) THEN 0 ELSE 1 END) ASC,
                      score DESC
            LIMIT  :limit OFFSET :offset
            """, nativeQuery = true)
    List<BusinessProfileSearchProjection> searchBusinessProfilesTiered(
            @Param("rawQuery") String rawQuery,
            @Param("prefixQuery") String prefixQuery,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query(value = """
            SELECT count(*)
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  (
                       b.search_vector        @@ websearch_to_tsquery('spanish', :rawQuery)
                   OR  b.search_vector_simple @@ to_tsquery('simple',            :prefixQuery)
                   )
            """, nativeQuery = true)
    long countSearchResultsTiered(@Param("rawQuery") String rawQuery, @Param("prefixQuery") String prefixQuery);

    @Query(value = """
            SELECT b.name         AS name,
                   b.slug         AS slug,
                   b.biz_type::text AS bizType
            FROM   business_profiles b
            WHERE  b.is_active = true
            ORDER  BY b.biz_type ASC, b.name ASC
            LIMIT  :limit OFFSET :offset
            """, nativeQuery = true)
    List<DirectoryEntryProjection> findAllForDirectory(@Param("limit") int limit, @Param("offset") int offset);

    @Query(value = """
            SELECT count(*)
            FROM   business_profiles b
            WHERE  b.is_active = true
            """, nativeQuery = true)
    long countAllForDirectory();

    @Query(value = """
            SELECT b.name           AS name,
                   b.slug           AS slug,
                   b.biz_type::text AS bizType
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  UPPER(LEFT(b.name, 1)) = UPPER(:letter)
            ORDER  BY b.name ASC
            LIMIT  :limit OFFSET :offset
            """, nativeQuery = true)
    List<DirectoryEntryProjection> findByFirstLetter(@Param("letter") String letter,
                                                     @Param("limit") int limit,
                                                     @Param("offset") int offset);

    @Query(value = """
            SELECT count(*)
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  UPPER(LEFT(b.name, 1)) = UPPER(:letter)
            """, nativeQuery = true)
    long countByFirstLetter(@Param("letter") String letter);

    @Query(value = """
            SELECT b.name           AS name,
                   b.slug           AS slug,
                   b.biz_type::text AS bizType
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.name !~* '^[a-záéíóúñü]'
            ORDER  BY b.name ASC
            LIMIT  :limit OFFSET :offset
            """, nativeQuery = true)
    List<DirectoryEntryProjection> findByNonAlphaStart(@Param("limit") int limit, @Param("offset") int offset);

    @Query(value = """
            SELECT count(*)
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.name !~* '^[a-záéíóúñü]'
            """, nativeQuery = true)
    long countByNonAlphaStart();

    @Query(value = """
            SELECT b.name           AS name,
                   b.slug           AS slug,
                   b.biz_type::text AS bizType
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.tags @> jsonb_build_array(:tag)
            ORDER  BY b.name ASC
            LIMIT  :limit OFFSET :offset
            """, nativeQuery = true)
    List<DirectoryEntryProjection> findByTag(@Param("tag") String tag,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);

    @Query(value = """
            SELECT count(*)
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.tags @> jsonb_build_array(:tag)
            """, nativeQuery = true)
    long countByTag(@Param("tag") String tag);

    @Query(value = """
            SELECT b.id::text       AS id,
                   b.name           AS name,
                   b.slug           AS slug,
                   b.biz_type::text AS bizType,
                   b.latitude       AS latitude,
                   b.longitude      AS longitude
            FROM   business_profiles b
            WHERE  b.is_active = true
              AND  b.latitude  IS NOT NULL
              AND  b.longitude IS NOT NULL
            ORDER  BY b.name ASC
            """, nativeQuery = true)
    List<BusinessLocationProjection> findAllWithLocation();

    @Query(value = """
            SELECT t.tag, count(*) AS cnt
            FROM   business_profiles b,
                   jsonb_array_elements_text(b.tags) AS t(tag)
            WHERE  b.is_active = true
              AND  jsonb_array_length(b.tags) > 0
            GROUP  BY t.tag
            ORDER  BY cnt DESC, t.tag ASC
            """, nativeQuery = true)
    List<TagCountProjection> findAllTagsWithCount();
}
