package com.paginass.repository;

import com.paginass.entity.BannerHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BannerHistoryRepository extends JpaRepository<BannerHistory, UUID> {

    List<BannerHistory> findAllByOrderByUploadedAtDesc();

    long countBy();
}
