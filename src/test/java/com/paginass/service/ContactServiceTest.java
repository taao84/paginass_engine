package com.paginass.service;

import com.paginass.dto.ContactMessagePageDto;
import com.paginass.dto.ContactMessageRequestDto;
import com.paginass.dto.ContactMessageSummaryDto;
import com.paginass.dto.ContactSettingsDto;
import com.paginass.entity.ContactMessage;
import com.paginass.entity.ContactSettings;
import com.paginass.entity.User;
import com.paginass.repository.ContactMessageRepository;
import com.paginass.repository.ContactSettingsRepository;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock private ContactSettingsRepository contactSettingsRepository;
    @Mock private ContactMessageRepository contactMessageRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ContactService contactService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@test.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user, String role) {
        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password("x")
                .authorities(new SimpleGrantedAuthority("ROLE_" + role))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities()));
    }

    // --- submit ---

    @Test
    void submit_storesMessage() {
        ContactMessageRequestDto dto = new ContactMessageRequestDto(
                "Ayuda con registro", "user@test.com", "No puedo crear mi cuenta.");

        contactService.submit(dto);

        verify(contactMessageRepository).save(argThat(msg ->
                "Ayuda con registro".equals(msg.getSubject()) &&
                "user@test.com".equals(msg.getReplyEmail()) &&
                "No puedo crear mi cuenta.".equals(msg.getMessage()) &&
                msg.getSubmittedAt() != null &&
                msg.getReadAt() == null
        ));
    }

    // --- listMessages ---

    @Test
    void listMessages_returnsNewestFirst() {
        authenticateAs(adminUser, "ADMIN");

        ContactMessage old = buildMessage("Viejo", OffsetDateTime.now().minusDays(2));
        ContactMessage recent = buildMessage("Reciente", OffsetDateTime.now());
        when(contactMessageRepository.findAllByOrderBySubmittedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recent, old)));

        ContactMessagePageDto result = contactService.listMessages(0);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).subject()).isEqualTo("Reciente");
        assertThat(result.content().get(1).subject()).isEqualTo("Viejo");
    }

    // --- markRead ---

    @Test
    void markRead_setsReadAtAndReadBy() {
        authenticateAs(adminUser, "ADMIN");
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

        ContactMessage msg = buildMessage("Test", OffsetDateTime.now().minusHours(1));
        when(contactMessageRepository.findById(msg.getId())).thenReturn(Optional.of(msg));
        when(contactMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContactMessageSummaryDto result = contactService.markRead(msg.getId());

        assertThat(result.readAt()).isNotNull();
        assertThat(msg.getReadBy()).isEqualTo(adminUser.getId());
    }

    @Test
    void markRead_doesNotOverwriteIfAlreadyRead() {
        authenticateAs(adminUser, "ADMIN");
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

        OffsetDateTime originalReadAt = OffsetDateTime.now().minusMinutes(10);
        ContactMessage msg = buildMessage("Already read", OffsetDateTime.now().minusHours(1));
        msg.setReadAt(originalReadAt);
        when(contactMessageRepository.findById(msg.getId())).thenReturn(Optional.of(msg));
        when(contactMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        contactService.markRead(msg.getId());

        assertThat(msg.getReadAt()).isEqualTo(originalReadAt);
    }

    // --- updateSettings ---

    @Test
    void updateSettings_persistsAllFields() {
        authenticateAs(adminUser, "ADMIN");
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

        ContactSettings existing = new ContactSettings();
        existing.setId(1);
        when(contactSettingsRepository.findById(1)).thenReturn(Optional.of(existing));
        when(contactSettingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContactSettingsDto dto = new ContactSettingsDto(
                "Estamos aquí para ayudarte.", "soporte@paginass.com", "+57 300 000 0000");

        ContactSettingsDto result = contactService.updateSettings(dto);

        assertThat(result.pageDescription()).isEqualTo("Estamos aquí para ayudarte.");
        assertThat(result.contactEmail()).isEqualTo("soporte@paginass.com");
        assertThat(result.contactPhone()).isEqualTo("+57 300 000 0000");
        assertThat(existing.getUpdatedBy()).isEqualTo(adminUser.getId());
    }

    // --- helpers ---

    private ContactMessage buildMessage(String subject, OffsetDateTime submittedAt) {
        ContactMessage msg = new ContactMessage();
        msg.setId(UUID.randomUUID());
        msg.setSubject(subject);
        msg.setReplyEmail("user@test.com");
        msg.setMessage("Mensaje de prueba.");
        msg.setSubmittedAt(submittedAt);
        return msg;
    }
}
