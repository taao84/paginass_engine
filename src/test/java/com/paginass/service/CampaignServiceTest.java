package com.paginass.service;

import com.paginass.dto.CampaignCreateRequestDto;
import com.paginass.dto.CampaignDto;
import com.paginass.dto.CampaignStatusUpdateRequestDto;
import com.paginass.entity.Campaign;
import com.paginass.entity.CampaignStatus;
import com.paginass.entity.CampaignType;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.CampaignBusinessRepository;
import com.paginass.repository.CampaignRepository;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignServiceTest {

    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignBusinessRepository campaignBusinessRepository;
    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private CampaignService campaignService;

    private User admin;

    @BeforeEach
    void setup() {
        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setEmail("admin@example.com");
        admin.setRole(UserRole.ADMIN);

        UserDetails principal = new org.springframework.security.core.userdetails.User(
                "admin@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(campaignBusinessRepository.findByCampaignIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_photoContest_persistsCampaignAndDetails() {
        when(campaignRepository.existsBySlug(any())).thenReturn(false);
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CampaignCreateRequestDto request = new CampaignCreateRequestDto(
                "Mi Concurso",
                "Descripción",
                CampaignType.PHOTO_CONTEST,
                OffsetDateTime.now().plusDays(30),
                "<p>Reglas <strong>importantes</strong></p>",
                "<p>Condiciones</p>",
                "<p>🏆 Premio: viaje a la ciudad</p>"
        );

        CampaignDto result = campaignService.create(request, null);

        assertThat(result.name()).isEqualTo("Mi Concurso");
        assertThat(result.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(result.slug()).isEqualTo("mi-concurso");
        assertThat(result.startDatetime()).isNull();
        assertThat(result.photoContestDetails()).isNotNull();
        assertThat(result.photoContestDetails().rules()).contains("<strong>importantes</strong>");
    }

    @Test
    void create_stripsMaliciousHtmlFromRules() {
        when(campaignRepository.existsBySlug(any())).thenReturn(false);
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CampaignCreateRequestDto request = new CampaignCreateRequestDto(
                "Concurso", null, CampaignType.PHOTO_CONTEST,
                OffsetDateTime.now().plusDays(10),
                "<p>Rules</p><script>alert('xss')</script>",
                "<p>Conditions</p>", null
        );

        CampaignDto result = campaignService.create(request, null);

        assertThat(result.photoContestDetails().rules()).doesNotContain("<script>");
        assertThat(result.photoContestDetails().rules()).doesNotContain("alert");
    }

    @Test
    void create_sanitizesAwardsAndPreservesEmoji() {
        when(campaignRepository.existsBySlug(any())).thenReturn(false);
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CampaignCreateRequestDto request = new CampaignCreateRequestDto(
                "Concurso", null, CampaignType.PHOTO_CONTEST,
                OffsetDateTime.now().plusDays(10),
                null, null, "<p>🏆 Premio<script>evil()</script></p>"
        );

        CampaignDto result = campaignService.create(request, null);

        assertThat(result.photoContestDetails().awards()).doesNotContain("<script>");
        assertThat(result.photoContestDetails().awards()).doesNotContain("evil");
        assertThat(result.photoContestDetails().awards()).containsAnyOf("🏆", "&#127942;", "&#x1f3c6;");
    }

    @Test
    void create_generatesUniqueSlugWhenBaseExists() {
        when(campaignRepository.existsBySlug("mi-concurso")).thenReturn(true);
        when(campaignRepository.existsBySlug("mi-concurso-2")).thenReturn(false);
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CampaignCreateRequestDto request = new CampaignCreateRequestDto(
                "Mi Concurso", null, CampaignType.PHOTO_CONTEST,
                OffsetDateTime.now().plusDays(10), null, null, null);

        CampaignDto result = campaignService.create(request, null);

        assertThat(result.slug()).isEqualTo("mi-concurso-2");
    }

    @Test
    void updateStatus_changesCampaignStatus() {
        Campaign campaign = buildCampaign(CampaignStatus.DRAFT);
        when(campaignRepository.findById(campaign.getId())).thenReturn(Optional.of(campaign));

        CampaignDto result = campaignService.updateStatus(campaign.getId(),
                new CampaignStatusUpdateRequestDto(CampaignStatus.ACTIVE));

        assertThat(result.status()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    void updateStatus_setsStartDatetimeOnFirstActivation() {
        Campaign campaign = buildCampaign(CampaignStatus.DRAFT);
        assertThat(campaign.getStartDatetime()).isNull();
        when(campaignRepository.findById(campaign.getId())).thenReturn(Optional.of(campaign));

        campaignService.updateStatus(campaign.getId(),
                new CampaignStatusUpdateRequestDto(CampaignStatus.ACTIVE));

        assertThat(campaign.getStartDatetime()).isNotNull();
    }

    @Test
    void updateStatus_doesNotOverwriteExistingStartDatetime() {
        Campaign campaign = buildCampaign(CampaignStatus.DRAFT);
        OffsetDateTime original = OffsetDateTime.now().minusDays(3);
        campaign.setStartDatetime(original);
        when(campaignRepository.findById(campaign.getId())).thenReturn(Optional.of(campaign));

        campaignService.updateStatus(campaign.getId(),
                new CampaignStatusUpdateRequestDto(CampaignStatus.ACTIVE));

        assertThat(campaign.getStartDatetime()).isEqualTo(original);
    }

    @Test
    void getById_throwsWhenNotFound() {
        UUID missing = UUID.randomUUID();
        when(campaignRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.getById(missing))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Campaign not found");
    }

    @Test
    void getBySlug_throwsWhenNotFound() {
        when(campaignRepository.findBySlug("no-such-slug")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.getBySlug("no-such-slug"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getBySlug_anyoneCanSeeDraftCampaignViaDirectLink() {
        SecurityContextHolder.clearContext();
        setAuthAs("user@example.com", "ROLE_CONTRIBUTOR");

        Campaign draft = buildCampaign(CampaignStatus.DRAFT);
        when(campaignRepository.findBySlug("draft-campaign")).thenReturn(Optional.of(draft));

        CampaignDto result = campaignService.getBySlug("draft-campaign");

        assertThat(result.status()).isEqualTo(CampaignStatus.DRAFT);
    }

    @Test
    void getBySlug_nonAdminCanSeeActiveCampaign() {
        SecurityContextHolder.clearContext();
        setAuthAs("user@example.com", "ROLE_CONTRIBUTOR");

        Campaign active = buildCampaign(CampaignStatus.ACTIVE);
        when(campaignRepository.findBySlug("active-campaign")).thenReturn(Optional.of(active));

        CampaignDto result = campaignService.getBySlug("active-campaign");

        assertThat(result.status()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    void listAll_returnsCampaignsInOrder() {
        Campaign c1 = buildCampaign(CampaignStatus.ACTIVE);
        c1.setName("A");
        when(campaignRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(c1));

        List<CampaignDto> result = campaignService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("A");
    }

    private Campaign buildCampaign(CampaignStatus status) {
        Campaign c = new Campaign();
        c.setId(UUID.randomUUID());
        c.setName("Test");
        c.setStatus(status);
        c.setSlug(status == CampaignStatus.DRAFT ? "draft-campaign" : "active-campaign");
        c.setEndDatetime(OffsetDateTime.now().plusDays(10));
        return c;
    }

    private void setAuthAs(String email, String role) {
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                email, "", List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
