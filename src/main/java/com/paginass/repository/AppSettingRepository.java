package com.paginass.repository;

import com.paginass.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
    List<AppSetting> findAllByIsGlobal(boolean isGlobal);
}
