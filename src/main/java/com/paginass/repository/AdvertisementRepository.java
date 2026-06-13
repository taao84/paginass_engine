package com.paginass.repository;

import com.paginass.entity.AdCategory;
import com.paginass.entity.Advertisement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AdvertisementRepository extends JpaRepository<Advertisement, UUID> {

    List<Advertisement> findAllByActiveTrueOrderByCreatedAtDesc();

    Page<Advertisement> findAllByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM Advertisement a ORDER BY a.active ASC, a.createdAt DESC")
    Page<Advertisement> findAllOrderByActiveAscCreatedAtDesc(Pageable pageable);

    List<Advertisement> findAllByCategoryAndActiveTrueOrderByCreatedAtDesc(AdCategory category);

    Page<Advertisement> findAllByCategoryAndActiveTrueOrderByCreatedAtDesc(AdCategory category, Pageable pageable);

    List<Advertisement> findAllByOwnerIdAndActiveTrueOrderByCreatedAtDesc(UUID ownerId);

    @Query(value = """
            SELECT * FROM advertisements
            WHERE active = true
              AND tags @> CAST(:tag AS jsonb)
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Advertisement> searchByTagAndActive(@Param("tag") String tagJson);

    @Query(value = """
            SELECT * FROM advertisements
            WHERE active = true
              AND (title ILIKE :kw OR content ILIKE :kw)
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Advertisement> searchByKeywordAndActive(@Param("kw") String keyword);

    @Query(value = """
            SELECT * FROM advertisements
            WHERE active = true
              AND (title ILIKE :kw OR content ILIKE :kw)
            ORDER BY created_at DESC
            """,
           countQuery = """
            SELECT count(*) FROM advertisements
            WHERE active = true
              AND (title ILIKE :kw OR content ILIKE :kw)
            """,
           nativeQuery = true)
    Page<Advertisement> searchByKeywordAndActive(@Param("kw") String keyword, Pageable pageable);

    List<Advertisement> findAllByActiveTrueAndExpiresAtBefore(Instant now);

    List<Advertisement> findByBusinessIdAndCategoryAndActiveTrueAndEventDateAfterOrderByEventDateAsc(
            UUID businessId, AdCategory category, Instant now);

    List<Advertisement> findByBusinessIdAndCategoryAndActiveTrueOrderByEventDateAsc(
            UUID businessId, AdCategory category);

    @Query(value = """
            SELECT a.id        AS id,
                   a.title     AS title,
                   a.category  AS category
            FROM   advertisements a
            WHERE  a.active = true
              AND  a.title ILIKE :q
            ORDER  BY a.created_at DESC
            LIMIT  10
            """, nativeQuery = true)
    List<HighlightableAdProjection> searchActiveByTitle(@Param("q") String q);
}
