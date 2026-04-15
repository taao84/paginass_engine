package com.paginass.service;

import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public User registerUser(String email, String rawPassword, String fullName) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = new User();
        user.setOauthProvider("local");
        user.setOauthId(email);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.CONTRIBUTOR);
        user.setEmailVerified(false);
        User saved = userRepository.save(user);
        emailVerificationService.generateAndSendToken(saved);
        return saved;
    }

    public String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String base = normalized.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return base + "-" + suffix;
    }
}
