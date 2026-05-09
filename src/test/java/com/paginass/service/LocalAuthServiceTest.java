package com.paginass.service;

import com.paginass.entity.User;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalAuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private AdminUserService adminUserService;

    @InjectMocks
    private LocalAuthService service;

    @Test
    void requestPasswordReset_triggersReset_whenLocalUserFound() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setOauthProvider("local");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        service.requestPasswordReset("user@example.com");

        verify(adminUserService).initiatePasswordReset(userId);
    }

    @Test
    void requestPasswordReset_doesNothing_whenEmailNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        service.requestPasswordReset("ghost@example.com");

        verify(adminUserService, never()).initiatePasswordReset(any());
    }

    @Test
    void requestPasswordReset_doesNothing_whenUserIsOAuthOnly() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("oauth@example.com");
        user.setOauthProvider("google");

        when(userRepository.findByEmail("oauth@example.com")).thenReturn(Optional.of(user));

        service.requestPasswordReset("oauth@example.com");

        verify(adminUserService, never()).initiatePasswordReset(any());
    }
}
