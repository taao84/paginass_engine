package com.paginass.controller;

import com.paginass.dto.AuthResponseDto;
import com.paginass.dto.CurrentUserDto;
import com.paginass.dto.RegisterRequestDto;
import com.paginass.dto.ResendVerificationRequestDto;
import com.paginass.entity.User;
import com.paginass.repository.UserRepository;
import com.paginass.service.EmailVerificationService;
import com.paginass.service.LocalAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LocalAuthService localAuthService;
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        User user = localAuthService.registerUser(request.email(), request.password(), request.fullName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponseDto(user.getId(), "Account created successfully"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        try {
            emailVerificationService.verifyToken(token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/verify-email?status=success"))
                    .build();
        } catch (EmailVerificationService.InvalidTokenException ex) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/verify-email?status=" + ex.getReason()))
                    .build();
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequestDto request) {
        emailVerificationService.resendVerification(request.email());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserDto> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = resolveEmail(authentication);
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(new CurrentUserDto(user.getFullName(), user.getEmail(), user.getRole())))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    private String resolveEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("email");
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName();
    }
}
