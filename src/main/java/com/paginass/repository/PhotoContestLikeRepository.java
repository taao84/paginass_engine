package com.paginass.repository;

import com.paginass.entity.PhotoContestLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PhotoContestLikeRepository extends JpaRepository<PhotoContestLike, UUID> {

    boolean existsByEntryIdAndUserId(UUID entryId, UUID userId);

    Optional<PhotoContestLike> findByEntryIdAndUserId(UUID entryId, UUID userId);

    long countByEntryId(UUID entryId);

    void deleteAllByEntryId(UUID entryId);
}
