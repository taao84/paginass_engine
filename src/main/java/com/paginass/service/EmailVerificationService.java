package com.paginass.service;

import com.paginass.entity.User;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
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

    @Autowired @Lazy
    private EmailVerificationService self;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void generateAndSendToken(User user) {
        String token = self.saveToken(user);
        sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public String saveToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
        userRepository.save(user);
        return token;
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

    public void resendVerification(String email) {
        String[] data = self.prepareResend(email);
        if (data != null) {
            sendVerificationEmail(data[0], data[1]);
        }
    }

    @Transactional
    public String[] prepareResend(String email) {
        return userRepository.findByEmail(email).map(user -> {
            if (user.isEmailVerified() || !LocalAuthService.LOCAL_PROVIDER.equals(user.getOauthProvider())) {
                return null;
            }
            if (user.getTokenExpiresAt() != null) {
                OffsetDateTime issuedAt = user.getTokenExpiresAt().minusHours(1);
                if (OffsetDateTime.now(ZoneOffset.UTC).isBefore(issuedAt.plusMinutes(5))) {
                    return null;
                }
            }
            String token = UUID.randomUUID().toString();
            user.setVerificationToken(token);
            user.setTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));
            userRepository.save(user);
            return new String[]{user.getEmail(), token};
        }).orElse(null);
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
