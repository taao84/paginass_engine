package com.paginass.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paginass.dto.BusinessProfileDetailDto;
import com.paginass.dto.ContributorBusinessCreateRequestDto;
import com.paginass.entity.BusinessProfile;
import com.paginass.entity.BusinessType;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.BusinessClaimRepository;
import com.paginass.repository.BusinessProfileEditRepository;
import com.paginass.repository.BusinessProfileRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

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
class ContributorBusinessCreationTest {

    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private BusinessClaimRepository businessClaimRepository;
    @Mock private BusinessProfileEditRepository businessProfileEditRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private ObjectMapper objectMapper;
    @Mock private LocalAuthService localAuthService;

    @InjectMocks
    private BusinessProfileService service;

    private User contributor;

    @BeforeEach
    void setup() {
        contributor = new User();
        contributor.setId(UUID.randomUUID());
        contributor.setEmail("contrib@example.com");
        contributor.setRole(UserRole.CONTRIBUTOR);

        var auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "contrib@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_CONTRIBUTOR"))),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CONTRIBUTOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail("contrib@example.com")).thenReturn(Optional.of(contributor));
        when(localAuthService.generateSlug(any())).thenReturn("my-business-abc12345");
        when(businessProfileRepository.save(any())).thenAnswer(inv -> {
            BusinessProfile p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            if (p.getBizType() == null) p.setBizType(BusinessType.INFORMAL);
            return p;
        });
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOwnedBusiness_happyPath_informal() {
        when(businessProfileRepository.existsByOwnerIdAndIsClaimedTrue(contributor.getId())).thenReturn(false);

        var request = new ContributorBusinessCreateRequestDto(
                "Mi Servicio", "INFORMAL", "desc", "3001234567", null, null, null,
                List.of("plomeria"), false, null, null);

        BusinessProfileDetailDto result = service.createOwnedBusiness(request);

        ArgumentCaptor<BusinessProfile> captor = ArgumentCaptor.forClass(BusinessProfile.class);
        verify(businessProfileRepository).save(captor.capture());
        BusinessProfile saved = captor.getValue();

        assertThat(saved.getBizType()).isEqualTo(BusinessType.INFORMAL);
        assertThat(saved.isClaimed()).isTrue();
        assertThat(saved.getOwnerId()).isEqualTo(contributor.getId());
        assertThat(saved.isMiniSiteEnabled()).isFalse();
        assertThat(result).isNotNull();
    }

    @Test
    void createOwnedBusiness_happyPath_gig() {
        when(businessProfileRepository.existsByOwnerIdAndIsClaimedTrue(contributor.getId())).thenReturn(false);

        var request = new ContributorBusinessCreateRequestDto(
                "Juan Pérez", "GIG", null, null, null, null, null, null, null, null, null);

        service.createOwnedBusiness(request);

        ArgumentCaptor<BusinessProfile> captor = ArgumentCaptor.forClass(BusinessProfile.class);
        verify(businessProfileRepository).save(captor.capture());
        BusinessProfile saved = captor.getValue();

        assertThat(saved.getBizType()).isEqualTo(BusinessType.GIG);
        assertThat(saved.isClaimed()).isTrue();
        assertThat(saved.getOwnerId()).isEqualTo(contributor.getId());
        assertThat(saved.isMiniSiteEnabled()).isFalse();
    }

    @Test
    void createOwnedBusiness_brickAndMortar_throws400() {
        var request = new ContributorBusinessCreateRequestDto(
                "Ferretería", "BRICK_AND_MORTAR", null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.createOwnedBusiness(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void createOwnedBusiness_duplicateOwner_throws409() {
        when(businessProfileRepository.existsByOwnerIdAndIsClaimedTrue(contributor.getId())).thenReturn(true);

        var request = new ContributorBusinessCreateRequestDto(
                "Mi Otro Negocio", "INFORMAL", null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.createOwnedBusiness(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void createOwnedBusiness_noRoleChange() {
        when(businessProfileRepository.existsByOwnerIdAndIsClaimedTrue(contributor.getId())).thenReturn(false);

        var request = new ContributorBusinessCreateRequestDto(
                "Mi Negocio", "GIG", null, null, null, null, null, null, null, null, null);

        service.createOwnedBusiness(request);

        assertThat(contributor.getRole()).isEqualTo(UserRole.CONTRIBUTOR);
    }
}
