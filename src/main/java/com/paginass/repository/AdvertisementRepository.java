package com.paginass.repository;

import com.paginass.entity.AdCategory;
import com.paginass.entity.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AdvertisementRepository extends JpaRepository<Advertisement, UUID> {

    List<Advertisement> findAllByExpiresAtAfterOrderByCreatedAtDesc(Instant now);

    List<Advertisement> findAllByCategoryAndExpiresAtAfterOrderByCreatedAtDesc(AdCategory category, Instant now);

    List<Advertisement> findAllByOwnerIdAndExpiresAtAfterOrderByCreatedAtDesc(UUID ownerId, Instant now);

    @Query(value = """
            SELECT * FROM advertisements
            WHERE expires_at > :now
              AND tags @> CAST(:tag AS jsonb)
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Advertisement> searchByTagAndActive(@Param("tag") String tagJson, @Param("now") Instant now);

    @Query(value = """
            SELECT * FROM advertisements
            WHERE expires_at > :now
              AND (title ILIKE :kw OR content ILIKE :kw)
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<Advertisement> searchByKeywordAndActive(@Param("kw") String keyword, @Param("now") Instant now);

    @Query(value = """
            SELECT * FROM advertisements
            WHERE expires_at < :now
              AND jsonb_array_length(image_urls) > 0
            """, nativeQuery = true)
    List<Advertisement> findExpiredAdsWithImages(@Param("now") Instant now);
}
