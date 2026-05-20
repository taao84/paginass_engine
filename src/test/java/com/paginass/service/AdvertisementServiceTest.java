package com.paginass.service;

import com.paginass.dto.AdvertisementRequestDto;
import com.paginass.dto.AdvertisementResponseDto;
import com.paginass.dto.AdvertisementSummaryDto;
import com.paginass.entity.AdCategory;
import com.paginass.entity.Advertisement;
import com.paginass.entity.User;
import com.paginass.repository.AdvertisementRepository;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(MockitoExtension.class)
class AdvertisementServiceTest {

    @Mock private AdvertisementRepository advertisementRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private AdvertisementService advertisementService;

    private User owner;

    @BeforeEach
    void setupSecurityContext() {
        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@example.com");
        owner.setFullName("Owner User");

        setAuthAs(owner);

        // @Lazy @Autowired self-injection is not wired by Mockito — set it manually.
        ReflectionTestUtils.setField(advertisementService, "self", advertisementService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- createAdvertisement ---

    @Test
    void createAdvertisement_setsExpiresAt30DaysFromNow() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(advertisementRepository.save(any())).thenAnswer(inv -> {
            Advertisement ad = inv.getArgument(0);
            ad.setId(UUID.randomUUID());
            return ad;
        });

        AdvertisementRequestDto request = new AdvertisementRequestDto(
                AdCategory.SALE, "Test Sale", "Description", BigDecimal.valueOf(100), null, List.of());

        AdvertisementResponseDto result = advertisementService.createAdvertisement(request);

        long daysUntilExpiry = ChronoUnit.DAYS.between(Instant.now(), result.expiresAt());
        assertThat(daysUntilExpiry).isBetween(29L, 30L);
    }

    @Test
    void createAdvertisement_eventCategory_expiresAtEndOfEventDay() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(advertisementRepository.save(any())).thenAnswer(inv -> {
            Advertisement ad = inv.getArgument(0);
            ad.setId(UUID.randomUUID());
            return ad;
        });

        Instant eventDate = Instant.now().plus(7, ChronoUnit.DAYS);
        AdvertisementRequestDto request = new AdvertisementRequestDto(
                AdCategory.EVENT, "Concert", "Description", null, eventDate, List.of());

        AdvertisementResponseDto result = advertisementService.createAdvertisement(request);

        Instant expectedExpiry = eventDate.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS);
        assertThat(result.expiresAt()).isEqualTo(expectedExpiry);
    }

    @Test
    void createAdvertisement_eventCategory_noEventDate_throws400() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        AdvertisementRequestDto request = new AdvertisementRequestDto(
                AdCategory.EVENT, "Concert", "Description", null, null, List.of());

        assertThatThrownBy(() -> advertisementService.createAdvertisement(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    // --- getActiveAdvertisements ---

    @Test
    void getActiveAdvertisements_returnsActiveAds() {
        Advertisement active = buildAd(AdCategory.SALE, owner, Instant.now().plus(10, ChronoUnit.DAYS));
        when(advertisementRepository.findAllByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(active));
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));

        List<AdvertisementSummaryDto> result = advertisementService.getActiveAdvertisements(null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void getActiveAdvertisements_filtersByCategory() {
        Advertisement jobAd = buildAd(AdCategory.JOB, owner, Instant.now().plus(5, ChronoUnit.DAYS));
        when(advertisementRepository.findAllByCategoryAndActiveTrueOrderByCreatedAtDesc(eq(AdCategory.JOB)))
                .thenReturn(List.of(jobAd));
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));

        List<AdvertisementSummaryDto> result = advertisementService.getActiveAdvertisements(AdCategory.JOB, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo(AdCategory.JOB);
    }

    // --- getAdvertisementById ---

    @Test
    void getAdvertisementById_inactiveAd_throws404() {
        Advertisement inactive = buildAd(AdCategory.SALE, owner, Instant.now().plus(10, ChronoUnit.DAYS));
        inactive.setActive(false);
        when(advertisementRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> advertisementService.getAdvertisementById(inactive.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void getAdvertisementById_activeAd_returnsDto() {
        Advertisement active = buildAd(AdCategory.SALE, owner, Instant.now().plus(10, ChronoUnit.DAYS));
        when(advertisementRepository.findById(active.getId())).thenReturn(Optional.of(active));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        AdvertisementResponseDto result = advertisementService.getAdvertisementById(active.getId());

        assertThat(result.id()).isEqualTo(active.getId());
    }

    // --- addImages ---

    @Test
    void addImages_jobCategory_throws400() {
        Advertisement ad = buildAd(AdCategory.JOB, owner, Instant.now().plus(30, ChronoUnit.DAYS));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        MockMultipartFile file = new MockMultipartFile("files", "img.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> advertisementService.addImages(ad.getId(), List.of(file)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    @Test
    void addImages_saleCategory_moreThan3_throws400() {
        Advertisement ad = buildAd(AdCategory.SALE, owner, Instant.now().plus(30, ChronoUnit.DAYS));
        ad.getImageUrls().addAll(List.of("url1", "url2", "url3"));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        MockMultipartFile file = new MockMultipartFile("files", "img.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> advertisementService.addImages(ad.getId(), List.of(file)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    @Test
    void addImages_eventCategory_moreThan1_throws400() {
        Advertisement ad = buildAd(AdCategory.EVENT, owner, Instant.now().plus(30, ChronoUnit.DAYS));
        ad.getImageUrls().add("poster.jpg");
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        MockMultipartFile file = new MockMultipartFile("files", "extra.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> advertisementService.addImages(ad.getId(), List.of(file)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    // --- deleteAdvertisement ---

    @Test
    void deleteAdvertisement_deletesAdAndImages() throws IOException {
        Advertisement ad = buildAd(AdCategory.SALE, owner, Instant.now().plus(10, ChronoUnit.DAYS));
        ad.getImageUrls().addAll(List.of("ads/img1.jpg", "ads/img2.jpg"));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        advertisementService.deleteAdvertisement(ad.getId());

        verify(advertisementRepository).delete(ad);
        verify(storageService).deleteFile("ads/img1.jpg");
        verify(storageService).deleteFile("ads/img2.jpg");
    }

    // --- Helpers ---

    private Advertisement buildAd(AdCategory category, User owner, Instant expiresAt) {
        Advertisement ad = new Advertisement();
        ad.setId(UUID.randomUUID());
        ad.setOwnerId(owner.getId());
        ad.setCategory(category);
        ad.setTitle("Test ad");
        ad.setContent("Content");
        ad.setExpiresAt(expiresAt);
        ad.setCreatedAt(Instant.now());
        ad.setImageUrls(new ArrayList<>());
        ad.setTags(new ArrayList<>());
        return ad;
    }

    private void setAuthAs(User user) {
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                user.getEmail(), "", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
