package com.paginass.repository;

import com.paginass.entity.CampaignBusiness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignBusinessRepository extends JpaRepository<CampaignBusiness, UUID> {

    List<CampaignBusiness> findByCampaignIdOrderByDisplayOrderAsc(UUID campaignId);

    boolean existsByCampaignIdAndBusinessId(UUID campaignId, UUID businessId);

    Optional<CampaignBusiness> findByCampaignIdAndBusinessId(UUID campaignId, UUID businessId);
}
