package com.paginass.repository;

import com.paginass.entity.Campaign;
import com.paginass.entity.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    Optional<Campaign> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Campaign> findAllByOrderByCreatedAtDesc();

    List<Campaign> findByStatusOrderByCreatedAtDesc(CampaignStatus status);

    List<Campaign> findByStatusAndNameContainingIgnoreCaseOrderByNameAsc(CampaignStatus status, String name);

}
