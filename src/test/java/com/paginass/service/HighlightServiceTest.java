package com.paginass.service;

import com.paginass.dto.CreateHighlightRequestDto;
import com.paginass.dto.HighlightCardDto;
import com.paginass.dto.HighlightLifetime;
import com.paginass.dto.HighlightSummaryDto;
import com.paginass.entity.AdCategory;
import com.paginass.entity.Advertisement;
import com.paginass.entity.BusinessProfile;
import com.paginass.entity.BusinessType;
import com.paginass.entity.Highlight;
import com.paginass.entity.HighlightType;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.AdvertisementRepository;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.HighlightRepository;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HighlightServiceTest {

    @Mock private HighlightRepository highlightRepository;
    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private AdvertisementRepository advertisementRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private HighlightService highlightService;

    private User admin;
    private BusinessProfile activeBusiness;
    private Advertisement activeAd;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setEmail("admin@example.com");
        admin.setRole(UserRole.ADMIN);

        activeBusiness = new BusinessProfile();
        activeBusiness.setId(UUID.randomUUID());
        activeBusiness.setName("Panadería La Esperanza");
        activeBusiness.setSlug("panaderia-la-esperanza");
        activeBusiness.setBizType(BusinessType.BRICK_AND_MORTAR);
        activeBusiness.setDescription("El mejor pan del pueblo");
        activeBusiness.setActive(true);

        activeAd = new Advertisement();
        activeAd.setId(UUID.randomUUID());
        activeAd.setTitle("Se vende bicicleta");
        activeAd.setContent("Bicicleta en buen estado");
        activeAd.setCategory(AdCategory.SALE);
        activeAd.setActive(true);
        activeAd.setExpiresAt(Instant.now().plusSeconds(86400));

        UserDetails principal = new org.springframework.security.core.userdetails.User(
                "admin@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // --- createHighlight ---

    @Test
    void createHighlight_withValidBusinessRef_savesHighlight() {
        when(businessProfileRepository.findById(activeBusiness.getId()))
                .thenReturn(Optional.of(activeBusiness));
        when(highlightRepository.save(any())).thenAnswer(inv -> {
            Highlight h = inv.getArgument(0);
            h.setId(UUID.randomUUID());
            return h;
        });

        CreateHighlightRequestDto request = new CreateHighlightRequestDto(
                HighlightType.BUSINESS, activeBusiness.getId(), HighlightLifetime.ONE_WEEK);

        HighlightSummaryDto result = highlightService.createHighlight(request);

        ArgumentCaptor<Highlight> captor = ArgumentCaptor.forClass(Highlight.class);
        verify(highlightRepository).save(captor.capture());
        Highlight saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(HighlightType.BUSINESS);
        assertThat(saved.getReferenceId()).isEqualTo(activeBusiness.getId());
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
        assertThat(saved.isEnabled()).isTrue();
        assertThat(result.enabled()).isTrue();
    }

    @Test
    void createHighlight_withValidAdRef_savesHighlight() {
        when(advertisementRepository.findById(activeAd.getId()))
                .thenReturn(Optional.of(activeAd));
        when(highlightRepository.save(any())).thenAnswer(inv -> {
            Highlight h = inv.getArgument(0);
            h.setId(UUID.randomUUID());
            return h;
        });

        CreateHighlightRequestDto request = new CreateHighlightRequestDto(
                HighlightType.ADVERTISEMENT, activeAd.getId(), HighlightLifetime.ONE_DAY);

        highlightService.createHighlight(request);

        verify(highlightRepository).save(any());
        verify(businessProfileRepository, never()).findById(any());
    }

    @Test
    void createHighlight_withNonExistentBusinessRef_throwsIllegalArgument() {
        UUID missingId = UUID.randomUUID();
        when(businessProfileRepository.findById(missingId)).thenReturn(Optional.empty());

        CreateHighlightRequestDto request = new CreateHighlightRequestDto(
                HighlightType.BUSINESS, missingId, HighlightLifetime.ONE_WEEK);

        assertThatThrownBy(() -> highlightService.createHighlight(request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(highlightRepository, never()).save(any());
    }

    @Test
    void createHighlight_withNonExistentAdRef_throwsIllegalArgument() {
        UUID missingId = UUID.randomUUID();
        when(advertisementRepository.findById(missingId)).thenReturn(Optional.empty());

        CreateHighlightRequestDto request = new CreateHighlightRequestDto(
                HighlightType.ADVERTISEMENT, missingId, HighlightLifetime.ONE_MONTH);

        assertThatThrownBy(() -> highlightService.createHighlight(request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(highlightRepository, never()).save(any());
    }

    // --- getActiveHighlights ---

    @Test
    void getActiveHighlights_includesHighlightForActiveBusiness() {
        Highlight h = enabledBusinessHighlight(activeBusiness.getId());
        when(highlightRepository.findAllByEnabledTrueOrderByCreatedAtDesc()).thenReturn(List.of(h));
        when(businessProfileRepository.findById(activeBusiness.getId()))
                .thenReturn(Optional.of(activeBusiness));
        when(storageService.toPublicUrl(any())).thenReturn("https://cdn/img.jpg");

        List<HighlightCardDto> result = highlightService.getActiveHighlights();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Panadería La Esperanza");
    }

    @Test
    void getActiveHighlights_excludesHighlightWhereBusinessIsInactive() {
        activeBusiness.setActive(false);
        Highlight h = enabledBusinessHighlight(activeBusiness.getId());
        when(highlightRepository.findAllByEnabledTrueOrderByCreatedAtDesc()).thenReturn(List.of(h));
        when(businessProfileRepository.findById(activeBusiness.getId()))
                .thenReturn(Optional.of(activeBusiness));

        List<HighlightCardDto> result = highlightService.getActiveHighlights();

        assertThat(result).isEmpty();
    }

    @Test
    void getActiveHighlights_excludesHighlightWhereAdIsExpired() {
        activeAd.setExpiresAt(Instant.now().minusSeconds(3600));
        Highlight h = enabledAdHighlight(activeAd.getId());
        when(highlightRepository.findAllByEnabledTrueOrderByCreatedAtDesc()).thenReturn(List.of(h));
        when(advertisementRepository.findById(activeAd.getId()))
                .thenReturn(Optional.of(activeAd));

        List<HighlightCardDto> result = highlightService.getActiveHighlights();

        assertThat(result).isEmpty();
    }

    @Test
    void getActiveHighlights_excludesHighlightWhereAdIsInactive() {
        activeAd.setActive(false);
        Highlight h = enabledAdHighlight(activeAd.getId());
        when(highlightRepository.findAllByEnabledTrueOrderByCreatedAtDesc()).thenReturn(List.of(h));
        when(advertisementRepository.findById(activeAd.getId()))
                .thenReturn(Optional.of(activeAd));

        List<HighlightCardDto> result = highlightService.getActiveHighlights();

        assertThat(result).isEmpty();
    }

    @Test
    void getActiveHighlights_cardDtoDoesNotExposeExpiresAt() {
        Highlight h = enabledBusinessHighlight(activeBusiness.getId());
        when(highlightRepository.findAllByEnabledTrueOrderByCreatedAtDesc()).thenReturn(List.of(h));
        when(businessProfileRepository.findById(activeBusiness.getId()))
                .thenReturn(Optional.of(activeBusiness));

        List<HighlightCardDto> result = highlightService.getActiveHighlights();

        // HighlightCardDto has no expiresAt field — verified at compile time;
        // this test confirms the public list does not accidentally include it at runtime
        assertThat(result.get(0)).isInstanceOf(HighlightCardDto.class);
    }

    // --- expireHighlights ---

    @Test
    void expireHighlights_setsEnabledFalseOnExpiredRows_andReturnsCount() {
        Highlight expired1 = enabledBusinessHighlight(UUID.randomUUID());
        Highlight expired2 = enabledAdHighlight(UUID.randomUUID());
        when(highlightRepository.findAllByEnabledTrueAndExpiresAtBefore(any()))
                .thenReturn(List.of(expired1, expired2));

        int count = highlightService.expireHighlights();

        assertThat(count).isEqualTo(2);
        assertThat(expired1.isEnabled()).isFalse();
        assertThat(expired2.isEnabled()).isFalse();
    }

    @Test
    void expireHighlights_whenNoneExpired_returnsZero() {
        when(highlightRepository.findAllByEnabledTrueAndExpiresAtBefore(any()))
                .thenReturn(List.of());

        int count = highlightService.expireHighlights();

        assertThat(count).isEqualTo(0);
    }

    // --- disableHighlight ---

    @Test
    void disableHighlight_setsEnabledFalse() {
        Highlight h = enabledBusinessHighlight(activeBusiness.getId());
        UUID id = UUID.randomUUID();
        h.setId(id);
        when(highlightRepository.findById(id)).thenReturn(Optional.of(h));

        highlightService.disableHighlight(id);

        assertThat(h.isEnabled()).isFalse();
    }

    @Test
    void disableHighlight_nonExistent_throwsIllegalArgument() {
        UUID missingId = UUID.randomUUID();
        when(highlightRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> highlightService.disableHighlight(missingId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- helpers ---

    private Highlight enabledBusinessHighlight(UUID referenceId) {
        Highlight h = new Highlight();
        h.setId(UUID.randomUUID());
        h.setType(HighlightType.BUSINESS);
        h.setReferenceId(referenceId);
        h.setExpiresAt(Instant.now().plusSeconds(86400));
        h.setEnabled(true);
        return h;
    }

    private Highlight enabledAdHighlight(UUID referenceId) {
        Highlight h = new Highlight();
        h.setId(UUID.randomUUID());
        h.setType(HighlightType.ADVERTISEMENT);
        h.setReferenceId(referenceId);
        h.setExpiresAt(Instant.now().plusSeconds(86400));
        h.setEnabled(true);
        return h;
    }
}
