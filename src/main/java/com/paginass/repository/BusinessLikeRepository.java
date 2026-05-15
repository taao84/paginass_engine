package com.paginass.repository;

import com.paginass.entity.BusinessLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BusinessLikeRepository extends JpaRepository<BusinessLike, UUID> {
    boolean existsByUserIdAndBusinessId(UUID userId, UUID businessId);
    long countByBusinessId(UUID businessId);
    void deleteByUserIdAndBusinessId(UUID userId, UUID businessId);
}
