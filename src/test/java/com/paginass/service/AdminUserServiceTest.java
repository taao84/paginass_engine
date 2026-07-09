package com.paginass.service;

import com.paginass.dto.AdminUserPageDto;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private MailService mailService;
    @Mock private SessionRegistry sessionRegistry;
    @Mock private SessionExpiryReasonStore sessionExpiryReasonStore;

    @InjectMocks
    private AdminUserService service;

    private User userWith(boolean enabled, boolean emailVerified) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setRole(UserRole.CONTRIBUTOR);
        user.setEnabled(enabled);
        user.setEmailVerified(emailVerified);
        user.setOauthProvider("local");
        user.setPhone("+1 555 0100");
        user.setDateOfBirth(LocalDate.of(1990, 1, 15));
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }

    @Test
    void listUsers_mapsEmailVerifiedTrue_intoDto() {
        User user = userWith(true, true);
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        AdminUserPageDto result = service.listUsers(null, null, null, null, 0);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).emailVerified()).isTrue();
        assertThat(result.content().get(0).enabled()).isTrue();
    }

    @Test
    void listUsers_mapsProfileFields_intoDto() {
        User user = userWith(true, true);
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        AdminUserPageDto result = service.listUsers(null, null, null, null, 0);

        var dto = result.content().get(0);
        assertThat(dto.oauthProvider()).isEqualTo("local");
        assertThat(dto.phone()).isEqualTo("+1 555 0100");
        assertThat(dto.dateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 15));
        assertThat(dto.updatedAt()).isNotNull();
    }

    @Test
    void listUsers_mapsEmailVerifiedFalse_intoDto_whenEnabledButUnverified() {
        // Reproduces the reported scenario: account is admin-enabled but never verified
        // (e.g. registration typo'd the email), so it must surface as unverified, not "active".
        User user = userWith(true, false);
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        AdminUserPageDto result = service.listUsers(null, null, null, null, 0);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).enabled()).isTrue();
        assertThat(result.content().get(0).emailVerified()).isFalse();
    }
}
