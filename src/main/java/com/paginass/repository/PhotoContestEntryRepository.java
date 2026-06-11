package com.paginass.repository;

import com.paginass.entity.PhotoContestEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoContestEntryRepository extends JpaRepository<PhotoContestEntry, UUID> {

    boolean existsByCampaignIdAndUserId(UUID campaignId, UUID userId);

    Optional<PhotoContestEntry> findByCampaignIdAndUserId(UUID campaignId, UUID userId);

    @Query(value = "SELECT * FROM photo_contest_entries WHERE campaign_id = :campaignId AND disabled = false ORDER BY RANDOM()", nativeQuery = true)
    List<PhotoContestEntry> findEnabledByCampaignIdRandomOrder(@Param("campaignId") UUID campaignId);

    @Query(value = "SELECT * FROM photo_contest_entries WHERE campaign_id = :campaignId ORDER BY disabled ASC, RANDOM()", nativeQuery = true)
    List<PhotoContestEntry> findAllByCampaignIdRandomOrder(@Param("campaignId") UUID campaignId);

    @Query("SELECT COUNT(l) FROM PhotoContestLike l WHERE l.entryId = :entryId")
    long countLikesByEntryId(@Param("entryId") UUID entryId);
}
