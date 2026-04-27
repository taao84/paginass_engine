package com.paginass.service;

import com.paginass.dto.AdminUserDto;
import com.paginass.dto.AdminUserPageDto;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int PAGE_SIZE = 20;
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final SessionRegistry sessionRegistry;

    @Autowired @Lazy
    private AdminUserService self;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public AdminUserPageDto listUsers(String search, UserRole role, Boolean isEnabled, int page) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("fullName")), pattern)
                ));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (isEnabled != null) {
                predicates.add(cb.equal(root.get("isEnabled"), isEnabled));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(
                spec, PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new AdminUserPageDto(
                userPage.getContent().stream().map(this::toDto).toList(),
                userPage.getNumber(),
                userPage.getTotalPages(),
                userPage.getTotalElements()
        );
    }

    @Transactional
    public AdminUserDto updateRole(UUID userId, UserRole newRole) {
        User user = findOrThrow(userId);
        user.setRole(newRole);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto setEnabled(UUID userId, boolean enabled) {
        User user = findOrThrow(userId);
        user.setEnabled(enabled);
        AdminUserDto result = toDto(userRepository.save(user));
        if (!enabled) {
            expireUserSessions(user.getEmail());
        }
        return result;
    }

    @Transactional
    public String generateTemporaryPassword(UUID userId) {
        User user = findOrThrow(userId);
        String tempPassword = randomAlphanumeric(12);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        return tempPassword;
    }

    public void initiatePasswordReset(UUID userId) {
        String[] data = self.preparePasswordReset(userId);
        String resetLink = frontendUrl + "/reset-password?token=" + data[1];
        mailService.sendPasswordResetEmail(data[0], resetLink);
    }

    @Transactional
    public String[] preparePasswordReset(UUID userId) {
        User user = findOrThrow(userId);
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(24));
        userRepository.save(user);
        return new String[]{ user.getEmail(), token };
    }

    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));

        if (OffsetDateTime.now(ZoneOffset.UTC).isAfter(user.getResetTokenExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
    }

    private void expireUserSessions(String email) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(p -> email.equals(extractPrincipalEmail(p)))
                .flatMap(p -> sessionRegistry.getAllSessions(p, false).stream())
                .forEach(SessionInformation::expireNow);
    }

    private static String extractPrincipalEmail(Object principal) {
        if (principal instanceof UserDetails ud) return ud.getUsername();
        if (principal instanceof OAuth2User oauth2) return oauth2.getAttribute("email");
        return null;
    }

    private User findOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AdminUserDto toDto(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }

    private static String randomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
