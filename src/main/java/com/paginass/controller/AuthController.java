package com.paginass.controller;

import com.paginass.dto.AuthResponseDto;
import com.paginass.dto.CurrentUserDto;
import com.paginass.dto.RegisterRequestDto;
import com.paginass.entity.User;
import com.paginass.repository.UserRepository;
import com.paginass.service.LocalAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LocalAuthService localAuthService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        User user = localAuthService.registerUser(request.email(), request.password(), request.fullName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponseDto(user.getId(), "Account created successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserDto> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = resolveEmail(authentication);
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(new CurrentUserDto(user.getFullName(), user.getEmail())))
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
