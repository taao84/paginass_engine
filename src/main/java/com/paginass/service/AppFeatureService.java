package com.paginass.service;

import com.paginass.dto.AppFeatureDto;
import com.paginass.entity.AppFeature;
import com.paginass.repository.AppFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppFeatureService {

    private final AppFeatureRepository appFeatureRepository;

    @Transactional(readOnly = true)
    public List<AppFeatureDto> getAll() {
        return appFeatureRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AppFeatureDto setEnabled(String key, boolean enabled) {
        AppFeature feature = appFeatureRepository.findById(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found: " + key));
        feature.setEnabled(enabled);
        return toDto(appFeatureRepository.save(feature));
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(String key) {
        return appFeatureRepository.findById(key)
                .map(AppFeature::isEnabled)
                .orElse(false);
    }

    private AppFeatureDto toDto(AppFeature f) {
        return new AppFeatureDto(f.getFeatureKey(), f.getDisplayName(), f.isEnabled());
    }
}
