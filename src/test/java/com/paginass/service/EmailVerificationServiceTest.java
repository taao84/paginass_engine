package com.paginass.service;

import com.paginass.entity.User;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private MailService mailService;

    @InjectMocks
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(service, "self", service);
    }

    @Test
    void generateAndSendToken_setsTokenExpiryInUTC() {
        User user = new User();
        user.setEmail("test@example.com");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(mailService).sendVerificationEmail(any(), any());

        service.generateAndSendToken(user);

        assertThat(user.getTokenExpiresAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(user.getTokenExpiresAt()).isAfter(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void verifyToken_throwsExpired_whenTokenIsInThePast() {
        User user = new User();
        user.setVerificationToken("tok");
        user.setTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(userRepository.findByVerificationToken("tok")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.verifyToken("tok"))
                .isInstanceOf(EmailVerificationService.InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verifyToken_succeeds_whenTokenIsStillValid() {
        User user = new User();
        user.setVerificationToken("tok");
        user.setTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        when(userRepository.findByVerificationToken("tok")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.verifyToken("tok");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getVerificationToken()).isNull();
    }

    @Test
    void resendVerification_isRateLimited_whenTokenIssuedLessThan5MinutesAgo() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setOauthProvider("local");
        user.setEmailVerified(false);
        // token expires in ~59 minutes, meaning it was issued ~1 minute ago (within 5min threshold)
        user.setTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(59));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        service.resendVerification("test@example.com");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(mailService);
    }

    @Test
    void resendVerification_sends_whenTokenIssuedMoreThan5MinutesAgo() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setOauthProvider("local");
        user.setEmailVerified(false);
        // token expires in ~50 minutes, meaning it was issued ~10 minutes ago (past threshold)
        user.setTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(50));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(mailService).sendVerificationEmail(any(), any());

        service.resendVerification("test@example.com");

        verify(mailService).sendVerificationEmail(eq("test@example.com"), any());
    }
}
