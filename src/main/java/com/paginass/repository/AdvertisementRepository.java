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

    List<Advertisement> findAllByActiveTrueOrderByCreatedAtDesc();

    List<Advertisement> findAllByCategoryAndActiveTrueOrderByCreatedAtDesc(AdCategory category);

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

    List<Advertisement> findAllByActiveTrueAndExpiresAtBefore(Instant now);
}
