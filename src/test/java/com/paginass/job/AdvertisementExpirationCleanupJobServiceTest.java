package com.paginass.job;

import com.paginass.entity.AdCategory;
import com.paginass.entity.Advertisement;
import com.paginass.repository.AdvertisementRepository;
import com.paginass.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvertisementExpirationCleanupJobServiceTest {

    @Mock private AdvertisementRepository advertisementRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private AdvertisementExpirationCleanupJobService jobService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(jobService, "self", jobService);
    }

    // --- demoteExpiredAds ---

    @Test
    void demoteExpiredAds_deactivatesAdWithImagesAndReturnsPaths() {
        Advertisement ad = buildExpiredAdWithImages("ads/img1.jpg", "ads/img2.jpg");
        when(advertisementRepository.findAllByActiveTrueAndExpiresAtBefore(any())).thenReturn(List.of(ad));

        List<String> paths = jobService.demoteExpiredAds();

        assertThat(paths).containsExactlyInAnyOrder("ads/img1.jpg", "ads/img2.jpg");
        assertThat(ad.getImageUrls()).isEmpty();
        assertThat(ad.isActive()).isFalse();
    }

    @Test
    void demoteExpiredAds_deactivatesAdWithoutImages() {
        Advertisement ad = buildExpiredAdWithImages();
        when(advertisementRepository.findAllByActiveTrueAndExpiresAtBefore(any())).thenReturn(List.of(ad));

        List<String> paths = jobService.demoteExpiredAds();

        assertThat(paths).isEmpty();
        assertThat(ad.isActive()).isFalse();
    }

    @Test
    void demoteExpiredAds_noExpiredAds_returnsEmptyList() {
        when(advertisementRepository.findAllByActiveTrueAndExpiresAtBefore(any())).thenReturn(List.of());

        List<String> paths = jobService.demoteExpiredAds();

        assertThat(paths).isEmpty();
    }

    // --- execute ---

    @Test
    void execute_deletesImagePathsFromStorage() throws IOException {
        Advertisement ad = buildExpiredAdWithImages("ads/a.jpg", "ads/b.jpg");
        when(advertisementRepository.findAllByActiveTrueAndExpiresAtBefore(any())).thenReturn(List.of(ad));

        jobService.execute();

        verify(storageService).deleteFile("ads/a.jpg");
        verify(storageService).deleteFile("ads/b.jpg");
    }

    @Test
    void execute_adWithoutImages_noStorageCallMade() throws IOException {
        Advertisement ad = buildExpiredAdWithImages();
        when(advertisementRepository.findAllByActiveTrueAndExpiresAtBefore(any())).thenReturn(List.of(ad));

        jobService.execute();

        verifyNoInteractions(storageService);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void execute_storageFailureOnOnePath_remainingPathsStillDeleted() throws IOException {
        Advertisement ad = buildExpiredAdWithImages("ads/good.jpg", "ads/bad.jpg");
        when(advertisementRepository.findAllByActiveTrueAndExpiresAtBefore(any())).thenReturn(List.of(ad));
        doThrow(new IOException("Storage error")).when(storageService).deleteFile("ads/bad.jpg");

        jobService.execute();

        verify(storageService).deleteFile("ads/good.jpg");
        verify(storageService).deleteFile("ads/bad.jpg");
    }

    // --- helpers ---

    private Advertisement buildExpiredAdWithImages(String... imagePaths) {
        Advertisement ad = new Advertisement();
        ad.setId(UUID.randomUUID());
        ad.setOwnerId(UUID.randomUUID());
        ad.setCategory(AdCategory.SALE);
        ad.setTitle("Expired ad");
        ad.setContent("content");
        ad.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        ad.setImageUrls(new ArrayList<>(List.of(imagePaths)));
        ad.setTags(new ArrayList<>());
        return ad;
    }
}
