package com.paginass.controller;

import com.paginass.dto.AuthResponseDto;
import com.paginass.dto.CurrentUserDto;
import com.paginass.dto.ForgotPasswordRequestDto;
import com.paginass.dto.RegisterRequestDto;
import com.paginass.dto.ResendVerificationRequestDto;
import com.paginass.dto.ResetPasswordConfirmRequestDto;
import com.paginass.dto.UpdateUserProfileRequestDto;
import com.paginass.entity.User;
import com.paginass.repository.UserRepository;
import com.paginass.service.AdminUserService;
import com.paginass.service.BusinessProfileService;
import com.paginass.service.EmailVerificationService;
import com.paginass.service.LocalAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LocalAuthService localAuthService;
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final AdminUserService adminUserService;
    private final BusinessProfileService businessProfileService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        User user = localAuthService.registerUser(request.email(), request.password(), request.fullName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponseDto(user.getId(), "Account created successfully"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        try {
            emailVerificationService.verifyToken(token);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (EmailVerificationService.InvalidTokenException ex) {
            return ResponseEntity.badRequest().body(Map.of("status", ex.getReason()));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequestDto request) {
        emailVerificationService.resendVerification(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        localAuthService.requestPasswordReset(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody ResetPasswordConfirmRequestDto request) {
        adminUserService.confirmPasswordReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserDto> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = resolveEmail(authentication);
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(new CurrentUserDto(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getRole(),
                        businessProfileService.hasOwnedBusiness(user.getId()),
                        user.getDateOfBirth(),
                        user.getPhone())))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PatchMapping("/me")
    public ResponseEntity<CurrentUserDto> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequestDto request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = resolveEmail(authentication);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));

        user.setFullName(request.fullName());
        user.setDateOfBirth(request.dateOfBirth());
        user.setPhone(request.phone() != null ? request.phone().trim() : null);
        userRepository.save(user);

        return ResponseEntity.ok(new CurrentUserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                businessProfileService.hasOwnedBusiness(user.getId()),
                user.getDateOfBirth(),
                user.getPhone()));
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
