package com.paginass.repository;

import com.paginass.entity.FeaturedProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeaturedProductRepository extends JpaRepository<FeaturedProduct, UUID> {

    List<FeaturedProduct> findByBusinessIdOrderByDisplayOrderAscCreatedAtAsc(UUID businessId);

    long countByBusinessId(UUID businessId);
}
