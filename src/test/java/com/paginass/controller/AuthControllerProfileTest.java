package com.paginass.controller;

import com.paginass.dto.CurrentUserDto;
import com.paginass.dto.UpdateUserProfileRequestDto;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.UserRepository;
import com.paginass.service.BusinessProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerProfileTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BusinessProfileService businessProfileService;

    @InjectMocks
    private AuthController authController;

    private User user;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setRole(UserRole.CONTRIBUTOR);
        user.setOauthProvider("local");
        user.setOauthId("local-id");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("test@example.com")
                .password("irrelevant")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CONTRIBUTOR")))
                .build();

        authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(businessProfileService.hasOwnedBusiness(any())).thenReturn(false);
    }

    @Test
    void updateProfile_persistsDateOfBirthAndPhone() {
        LocalDate dob = LocalDate.of(1990, 5, 15);
        UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto("Test User", dob, "+57 300 123 4567");

        ResponseEntity<CurrentUserDto> response = authController.updateProfile(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getDateOfBirth()).isEqualTo(dob);
        assertThat(saved.getPhone()).isEqualTo("+57 300 123 4567");
    }

    @Test
    void updateProfile_returnsDateOfBirthAndPhoneInResponse() {
        LocalDate dob = LocalDate.of(1985, 12, 1);
        UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto("Test User", dob, "3001234567");

        ResponseEntity<CurrentUserDto> response = authController.updateProfile(request, authentication);

        CurrentUserDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.dateOfBirth()).isEqualTo(dob);
        assertThat(body.phone()).isEqualTo("3001234567");
    }

    @Test
    void updateProfile_nullableFieldsAreCleared() {
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPhone("999");

        UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto("Test User", null, null);

        ResponseEntity<CurrentUserDto> response = authController.updateProfile(request, authentication);

        CurrentUserDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.dateOfBirth()).isNull();
        assertThat(body.phone()).isNull();
    }

    @Test
    void updateProfile_phoneIsTrimmed() {
        UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto("Test User", null, "  3001234567  ");

        authController.updateProfile(request, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPhone()).isEqualTo("3001234567");
    }

    @Test
    void updateProfile_unauthenticated_returns401() {
        UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto("X", null, null);

        ResponseEntity<CurrentUserDto> response = authController.updateProfile(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
