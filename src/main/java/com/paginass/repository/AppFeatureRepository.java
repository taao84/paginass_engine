package com.paginass.repository;

import com.paginass.entity.AppFeature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppFeatureRepository extends JpaRepository<AppFeature, String> {
}
