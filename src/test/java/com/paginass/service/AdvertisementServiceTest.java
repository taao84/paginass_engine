package com.paginass.service;

import com.paginass.dto.AdvertisementRequestDto;
import com.paginass.dto.AdvertisementResponseDto;
import com.paginass.dto.AdvertisementSummaryDto;
import com.paginass.dto.AdvertisementUpdateRequestDto;
import com.paginass.entity.AdCategory;
import com.paginass.entity.Advertisement;
import com.paginass.entity.User;
import com.paginass.repository.AdvertisementRepository;
import com.paginass.repository.BusinessProfileRepository;
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

import com.paginass.dto.AdvertisementAdminPageDto;
import com.paginass.dto.PublicAdsPageDto;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(MockitoExtension.class)
class AdvertisementServiceTest {

    @Mock private AdvertisementRepository advertisementRepository;
    @Mock private UserRepository userRepository;
    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private StorageService storageService;
    @Mock private BusinessAuthorizationService businessAuthorizationService;

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
                AdCategory.SALE, "Test Sale", "Description", BigDecimal.valueOf(100), null, List.of(), null);

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
                AdCategory.EVENT, "Concert", "Description", null, eventDate, List.of(), null);

        AdvertisementResponseDto result = advertisementService.createAdvertisement(request);

        Instant expectedExpiry = eventDate.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS);
        assertThat(result.expiresAt()).isEqualTo(expectedExpiry);
    }

    @Test
    void createAdvertisement_eventCategory_noEventDate_throws400() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        AdvertisementRequestDto request = new AdvertisementRequestDto(
                AdCategory.EVENT, "Concert", "Description", null, null, List.of(), null);

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
    void addImages_saleCategory_moreThan4_throws400() {
        Advertisement ad = buildAd(AdCategory.SALE, owner, Instant.now().plus(30, ChronoUnit.DAYS));
        ad.getImageUrls().addAll(List.of("url1", "url2", "url3", "url4"));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        MockMultipartFile file = new MockMultipartFile("files", "img.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> advertisementService.addImages(ad.getId(), List.of(file)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    @Test
    void addImages_saleCategory_4thImage_succeeds() throws IOException {
        Advertisement ad = buildAd(AdCategory.SALE, owner, Instant.now().plus(30, ChronoUnit.DAYS));
        ad.getImageUrls().addAll(List.of("url1", "url2", "url3"));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        MockMultipartFile file = new MockMultipartFile("files", "img4.jpg", "image/jpeg", new byte[]{1});

        AdvertisementResponseDto result = advertisementService.addImages(ad.getId(), List.of(file));

        assertThat(result.imageUrls()).hasSize(4);
    }

    @Test
    void addImages_eventCategory_moreThan4_throws400() {
        Advertisement ad = buildAd(AdCategory.EVENT, owner, Instant.now().plus(30, ChronoUnit.DAYS));
        ad.getImageUrls().addAll(List.of("url1", "url2", "url3", "url4"));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        MockMultipartFile file = new MockMultipartFile("files", "extra.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> advertisementService.addImages(ad.getId(), List.of(file)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    @Test
    void addImages_eventCategory_4thImage_succeeds() throws IOException {
        Advertisement ad = buildAd(AdCategory.EVENT, owner, Instant.now().plus(30, ChronoUnit.DAYS));
        ad.getImageUrls().addAll(List.of("url1", "url2", "url3"));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        MockMultipartFile file = new MockMultipartFile("files", "img4.jpg", "image/jpeg", new byte[]{1});

        AdvertisementResponseDto result = advertisementService.addImages(ad.getId(), List.of(file));

        assertThat(result.imageUrls()).hasSize(4);
    }

    // --- deleteImage ---

    @Test
    void deleteImage_removesFromListAndStorage() throws IOException {
        Advertisement ad = buildAd(AdCategory.SALE, owner, Instant.now().plus(30, ChronoUnit.DAYS));
        ad.getImageUrls().addAll(List.of("ads/a.jpg", "ads/b.jpg"));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        advertisementService.deleteImage(ad.getId(), 0);

        verify(storageService).deleteFile("ads/a.jpg");
    }

    @Test
    void deleteImage_outOfRange_throws400() {
        Advertisement ad = buildAd(AdCategory.SALE, owner, Instant.now().plus(30, ChronoUnit.DAYS));
        ad.getImageUrls().add("ads/a.jpg");
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> advertisementService.deleteImage(ad.getId(), 5))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    // --- reorderImages ---

    @Test
    void reorderImages_validPermutation_reorders() {
        Advertisement ad = buildAd(AdCategory.SALE, owner, Instant.now().plus(30, ChronoUnit.DAYS));
        ad.getImageUrls().addAll(List.of("ads/a.jpg", "ads/b.jpg", "ads/c.jpg"));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        AdvertisementResponseDto result = advertisementService.reorderImages(ad.getId(), List.of(2, 0, 1));

        assertThat(result.imageUrls()).hasSize(3);
    }

    @Test
    void reorderImages_invalidPermutation_throws400() {
        Advertisement ad = buildAd(AdCategory.SALE, owner, Instant.now().plus(30, ChronoUnit.DAYS));
        ad.getImageUrls().addAll(List.of("ads/a.jpg", "ads/b.jpg"));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> advertisementService.reorderImages(ad.getId(), List.of(0, 5)))
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

    // --- updateAdvertisement ---

    @Test
    void updateAdvertisement_updatesFields() {
        Advertisement ad = buildAd(AdCategory.SALE, owner, Instant.now().plus(30, ChronoUnit.DAYS));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        AdvertisementUpdateRequestDto request = new AdvertisementUpdateRequestDto(
                "New Title", "New Content", BigDecimal.valueOf(50), null, List.of("tag1"));

        AdvertisementResponseDto result = advertisementService.updateAdvertisement(ad.getId(), request);

        assertThat(result.title()).isEqualTo("New Title");
        assertThat(result.content()).isEqualTo("New Content");
        assertThat(result.price()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void updateAdvertisement_eventCategoryWithoutDate_throws400() {
        Advertisement ad = buildAd(AdCategory.EVENT, owner, Instant.now().plus(7, ChronoUnit.DAYS));
        ad.setEventDate(Instant.now().plus(7, ChronoUnit.DAYS));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        AdvertisementUpdateRequestDto request = new AdvertisementUpdateRequestDto(
                "Title", "Content", null, null, List.of());

        assertThatThrownBy(() -> advertisementService.updateAdvertisement(ad.getId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    // --- createAdvertisement with businessId (BREQ039) ---

    @Test
    void createAdvertisement_withBusinessId_nonEventCategory_throwsBadRequest() {
        UUID businessId = UUID.randomUUID();

        AdvertisementRequestDto request = new AdvertisementRequestDto(
                AdCategory.SALE, "Job Ad", "Content", null, null, List.of(), businessId);

        assertThatThrownBy(() -> advertisementService.createAdvertisement(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(BAD_REQUEST);
    }

    @Test
    void createAdvertisement_withBusinessId_eventCategory_setsBusinessId() {
        UUID businessId = UUID.randomUUID();
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(advertisementRepository.save(any())).thenAnswer(inv -> {
            Advertisement ad = inv.getArgument(0);
            ad.setId(UUID.randomUUID());
            return ad;
        });

        Instant eventDate = Instant.now().plus(7, ChronoUnit.DAYS);
        AdvertisementRequestDto request = new AdvertisementRequestDto(
                AdCategory.EVENT, "Business Concert", "Come join us", null, eventDate, List.of(), businessId);

        AdvertisementResponseDto result = advertisementService.createAdvertisement(request);

        assertThat(result.businessId()).isEqualTo(businessId);
    }

    // --- getBusinessEvents (BREQ039) ---

    @Test
    void getBusinessEvents_returnsAllActiveEvents() {
        UUID businessId = UUID.randomUUID();
        Advertisement future = buildEventAd(businessId, Instant.now().plus(3, ChronoUnit.DAYS));
        Advertisement past   = buildEventAd(businessId, Instant.now().minus(1, ChronoUnit.DAYS));
        when(advertisementRepository
                .findByBusinessIdAndCategoryAndActiveTrueOrderByEventDateAsc(
                        eq(businessId), eq(AdCategory.EVENT)))
                .thenReturn(List.of(past, future));
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));

        List<AdvertisementSummaryDto> result = advertisementService.getBusinessEvents(businessId);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> businessId.equals(r.businessId()));
    }

    @Test
    void getBusinessEvents_noEvents_returnsEmpty() {
        UUID businessId = UUID.randomUUID();
        when(advertisementRepository
                .findByBusinessIdAndCategoryAndActiveTrueOrderByEventDateAsc(
                        eq(businessId), eq(AdCategory.EVENT)))
                .thenReturn(List.of());

        List<AdvertisementSummaryDto> result = advertisementService.getBusinessEvents(businessId);

        assertThat(result).isEmpty();
    }

    // --- getAdminAds ---

    @Test
    void getAdminAds_returnsActiveAndInactiveAds() {
        Advertisement active = buildAd(AdCategory.SALE, owner, Instant.now().plus(10, ChronoUnit.DAYS));
        Advertisement inactive = buildAd(AdCategory.JOB, owner, Instant.now().plus(20, ChronoUnit.DAYS));
        inactive.setActive(false);
        PageImpl<Advertisement> page = new PageImpl<>(List.of(inactive, active), PageRequest.of(0, 20), 2);
        when(advertisementRepository.findAllOrderByActiveAscCreatedAtDesc(any(Pageable.class))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));

        AdvertisementAdminPageDto result = advertisementService.getAdminAds(0);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).active()).isFalse();
        assertThat(result.content().get(1).active()).isTrue();
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void getAdminAds_noAds_returnsEmptyPage() {
        PageImpl<Advertisement> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(advertisementRepository.findAllOrderByActiveAscCreatedAtDesc(any(Pageable.class))).thenReturn(empty);

        AdvertisementAdminPageDto result = advertisementService.getAdminAds(0);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    // --- disableAdvertisement / enableAdvertisement ---

    @Test
    void disableAdvertisement_setsActiveFalse() {
        Advertisement ad = buildAd(AdCategory.SALE, owner, Instant.now().plus(10, ChronoUnit.DAYS));
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        advertisementService.disableAdvertisement(ad.getId());

        assertThat(ad.isActive()).isFalse();
    }

    @Test
    void enableAdvertisement_setsActiveTrue() {
        Advertisement ad = buildAd(AdCategory.SALE, owner, Instant.now().plus(10, ChronoUnit.DAYS));
        ad.setActive(false);
        when(advertisementRepository.findById(ad.getId())).thenReturn(Optional.of(ad));

        advertisementService.enableAdvertisement(ad.getId());

        assertThat(ad.isActive()).isTrue();
    }

    // --- getActiveAdvertisementsPage ---

    @Test
    void getActiveAdvertisementsPage_page0_returnsFirstBatch() {
        List<Advertisement> batch = List.of(
                buildAd(AdCategory.SALE, owner, Instant.now().plus(10, ChronoUnit.DAYS)),
                buildAd(AdCategory.JOB, owner, Instant.now().plus(10, ChronoUnit.DAYS)));
        PageImpl<Advertisement> page = new PageImpl<>(batch, PageRequest.of(0, 6), 8);
        when(advertisementRepository.findAllByActiveTrueOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));

        PublicAdsPageDto result = advertisementService.getActiveAdvertisementsPage(null, null, 0);

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.hasMore()).isTrue();
    }

    @Test
    void getActiveAdvertisementsPage_lastPage_hasMoreFalse() {
        List<Advertisement> batch = List.of(buildAd(AdCategory.SALE, owner, Instant.now().plus(10, ChronoUnit.DAYS)));
        PageImpl<Advertisement> page = new PageImpl<>(batch, PageRequest.of(1, 6), 7);
        when(advertisementRepository.findAllByActiveTrueOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));

        PublicAdsPageDto result = advertisementService.getActiveAdvertisementsPage(null, null, 1);

        assertThat(result.hasMore()).isFalse();
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void getActiveAdvertisementsPage_categoryFilter_delegatesToCategoryRepository() {
        Advertisement jobAd = buildAd(AdCategory.JOB, owner, Instant.now().plus(5, ChronoUnit.DAYS));
        PageImpl<Advertisement> page = new PageImpl<>(List.of(jobAd), PageRequest.of(0, 6), 1);
        when(advertisementRepository.findAllByCategoryAndActiveTrueOrderByCreatedAtDesc(
                eq(AdCategory.JOB), any(Pageable.class))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));

        PublicAdsPageDto result = advertisementService.getActiveAdvertisementsPage(AdCategory.JOB, null, 0);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).category()).isEqualTo(AdCategory.JOB);
        assertThat(result.hasMore()).isFalse();
    }

    @Test
    void getActiveAdvertisementsPage_searchFilter_delegatesToSearchRepository() {
        Advertisement ad = buildAd(AdCategory.SALE, owner, Instant.now().plus(5, ChronoUnit.DAYS));
        PageImpl<Advertisement> page = new PageImpl<>(List.of(ad), PageRequest.of(0, 6), 1);
        when(advertisementRepository.searchByKeywordAndActive(eq("%bike%"), any(Pageable.class))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(List.of(owner));

        PublicAdsPageDto result = advertisementService.getActiveAdvertisementsPage(null, "bike", 0);

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasMore()).isFalse();
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

    private Advertisement buildEventAd(UUID businessId, Instant eventDate) {
        Advertisement ad = buildAd(AdCategory.EVENT, owner, eventDate.plus(1, ChronoUnit.DAYS));
        ad.setBusinessId(businessId);
        ad.setEventDate(eventDate);
        return ad;
    }

    private void setAuthAs(User user) {
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                user.getEmail(), "", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
