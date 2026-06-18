package com.paginass.service;

import com.paginass.dto.BusinessConfigurationDto;
import com.paginass.entity.BusinessConfiguration;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.BusinessConfigurationRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class BusinessConfigurationServiceTest {

    @Mock private BusinessConfigurationRepository businessConfigurationRepository;
    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AdminBusinessConfigurationService adminBusinessConfigurationService;

    private UUID businessId;
    private User adminUser;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setOauthProvider("local");
        adminUser.setOauthId("local-admin");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("admin@example.com")
                .password("irrelevant")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // getConfiguration
    // -------------------------------------------------------------------------

    @Test
    void getConfiguration_returnsNullWhenNoRowExists() {
        when(businessProfileRepository.existsById(businessId)).thenReturn(true);
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.empty());

        BusinessConfigurationDto result = adminBusinessConfigurationService.getConfiguration(businessId);

        assertThat(result.maxFeaturedProducts()).isNull();
    }

    @Test
    void getConfiguration_returnsStoredValueWhenRowExists() {
        BusinessConfiguration config = new BusinessConfiguration();
        config.setBusinessId(businessId);
        config.setMaxFeaturedProducts(12);

        when(businessProfileRepository.existsById(businessId)).thenReturn(true);
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.of(config));

        BusinessConfigurationDto result = adminBusinessConfigurationService.getConfiguration(businessId);

        assertThat(result.maxFeaturedProducts()).isEqualTo(12);
    }

    @Test
    void getConfiguration_throwsNotFoundWhenBusinessDoesNotExist() {
        when(businessProfileRepository.existsById(businessId)).thenReturn(false);

        assertThatThrownBy(() -> adminBusinessConfigurationService.getConfiguration(businessId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }

    // -------------------------------------------------------------------------
    // updateConfiguration
    // -------------------------------------------------------------------------

    @Test
    void updateConfiguration_createsNewRowWhenNoExistingRow() {
        when(businessProfileRepository.existsById(businessId)).thenReturn(true);
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.empty());
        when(businessConfigurationRepository.save(any(BusinessConfiguration.class))).thenAnswer(i -> i.getArgument(0));

        BusinessConfigurationDto request = new BusinessConfigurationDto(8);
        BusinessConfigurationDto result = adminBusinessConfigurationService.updateConfiguration(businessId, request);

        assertThat(result.maxFeaturedProducts()).isEqualTo(8);

        ArgumentCaptor<BusinessConfiguration> captor = ArgumentCaptor.forClass(BusinessConfiguration.class);
        verify(businessConfigurationRepository).save(captor.capture());
        assertThat(captor.getValue().getBusinessId()).isEqualTo(businessId);
        assertThat(captor.getValue().getMaxFeaturedProducts()).isEqualTo(8);
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(adminUser.getId());
    }

    @Test
    void updateConfiguration_updatesExistingRow() {
        BusinessConfiguration existing = new BusinessConfiguration();
        existing.setBusinessId(businessId);
        existing.setMaxFeaturedProducts(4);

        when(businessProfileRepository.existsById(businessId)).thenReturn(true);
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.of(existing));
        when(businessConfigurationRepository.save(any(BusinessConfiguration.class))).thenAnswer(i -> i.getArgument(0));

        BusinessConfigurationDto result = adminBusinessConfigurationService.updateConfiguration(businessId, new BusinessConfigurationDto(15));

        assertThat(result.maxFeaturedProducts()).isEqualTo(15);

        ArgumentCaptor<BusinessConfiguration> captor = ArgumentCaptor.forClass(BusinessConfiguration.class);
        verify(businessConfigurationRepository).save(captor.capture());
        assertThat(captor.getValue().getMaxFeaturedProducts()).isEqualTo(15);
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(adminUser.getId());
    }

    @Test
    void updateConfiguration_nullMaxFeaturedProductsRevertsToGlobalDefault() {
        BusinessConfiguration existing = new BusinessConfiguration();
        existing.setBusinessId(businessId);
        existing.setMaxFeaturedProducts(10);

        when(businessProfileRepository.existsById(businessId)).thenReturn(true);
        when(businessConfigurationRepository.findById(businessId)).thenReturn(Optional.of(existing));
        when(businessConfigurationRepository.save(any(BusinessConfiguration.class))).thenAnswer(i -> i.getArgument(0));

        BusinessConfigurationDto result = adminBusinessConfigurationService.updateConfiguration(businessId, new BusinessConfigurationDto(null));

        assertThat(result.maxFeaturedProducts()).isNull();

        ArgumentCaptor<BusinessConfiguration> captor = ArgumentCaptor.forClass(BusinessConfiguration.class);
        verify(businessConfigurationRepository).save(captor.capture());
        assertThat(captor.getValue().getMaxFeaturedProducts()).isNull();
    }

    @Test
    void updateConfiguration_throwsNotFoundWhenBusinessDoesNotExist() {
        when(businessProfileRepository.existsById(businessId)).thenReturn(false);

        assertThatThrownBy(() -> adminBusinessConfigurationService.updateConfiguration(businessId, new BusinessConfigurationDto(5)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }
}
