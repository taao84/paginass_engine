package com.paginass.service;

import com.paginass.dto.AppFeatureDto;
import com.paginass.entity.AppFeature;
import com.paginass.repository.AppFeatureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppFeatureServiceTest {

    @Mock
    private AppFeatureRepository appFeatureRepository;

    @InjectMocks
    private AppFeatureService appFeatureService;

    private AppFeature feature;

    @BeforeEach
    void setUp() {
        feature = new AppFeature();
        feature.setFeatureKey("classic_directory");
        feature.setDisplayName("Directorio clásico");
        feature.setEnabled(true);
    }

    @Test
    void getAll_returnsAllFeatures() {
        when(appFeatureRepository.findAll()).thenReturn(List.of(feature));

        List<AppFeatureDto> result = appFeatureService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).featureKey()).isEqualTo("classic_directory");
        assertThat(result.get(0).displayName()).isEqualTo("Directorio clásico");
        assertThat(result.get(0).enabled()).isTrue();
    }

    @Test
    void setEnabled_updatesToggle() {
        when(appFeatureRepository.findById("classic_directory")).thenReturn(Optional.of(feature));
        when(appFeatureRepository.save(feature)).thenReturn(feature);

        AppFeatureDto result = appFeatureService.setEnabled("classic_directory", false);

        assertThat(result.enabled()).isFalse();
        verify(appFeatureRepository).save(feature);
    }

    @Test
    void setEnabled_unknownKey_throws404() {
        when(appFeatureRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appFeatureService.setEnabled("unknown", false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void isEnabled_returnsCorrectState() {
        when(appFeatureRepository.findById("classic_directory")).thenReturn(Optional.of(feature));

        assertThat(appFeatureService.isEnabled("classic_directory")).isTrue();

        feature.setEnabled(false);
        assertThat(appFeatureService.isEnabled("classic_directory")).isFalse();
    }

    @Test
    void isEnabled_unknownKey_returnsFalse() {
        when(appFeatureRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThat(appFeatureService.isEnabled("nonexistent")).isFalse();
    }
}
