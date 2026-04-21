package com.paginass.service;

import com.paginass.entity.User;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final MailService mailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public void generateAndSendToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        userRepository.save(user);
        sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public void verifyToken(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new InvalidTokenException("invalid"));

        if (OffsetDateTime.now(ZoneOffset.UTC).isAfter(user.getTokenExpiresAt())) {
            throw new InvalidTokenException("expired");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isEmailVerified() || !"local".equals(user.getOauthProvider())) {
                return;
            }
            if (user.getTokenExpiresAt() != null) {
                OffsetDateTime issuedAt = user.getTokenExpiresAt().minusHours(1);
                if (OffsetDateTime.now(ZoneOffset.UTC).isBefore(issuedAt.plusMinutes(5))) {
                    return; // rate-limit: token issued less than 5 minutes ago
                }
            }
            generateAndSendToken(user);
        });
    }

    private void sendVerificationEmail(String toEmail, String token) {
        String verifyLink = frontendUrl + "/verify-email?token=" + token;
        mailService.sendVerificationEmail(toEmail, verifyLink);
    }

    public static class InvalidTokenException extends RuntimeException {
        private final String reason;

        public InvalidTokenException(String reason) {
            super("Token " + reason);
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
}
