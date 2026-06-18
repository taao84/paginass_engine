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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactService {

    private static final int SETTINGS_ID = 1;
    private static final int PAGE_SIZE = 20;

    private final ContactSettingsRepository contactSettingsRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ContactSettingsDto getSettings() {
        return contactSettingsRepository.findById(SETTINGS_ID)
                .map(this::toSettingsDto)
                .orElse(new ContactSettingsDto(null, null, null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ContactSettingsDto updateSettings(ContactSettingsDto dto) {
        User admin = resolveCurrentUser();
        ContactSettings settings = contactSettingsRepository.findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("contact_settings seed row missing"));
        settings.setPageDescription(dto.pageDescription());
        settings.setContactEmail(dto.contactEmail());
        settings.setContactPhone(dto.contactPhone());
        settings.setUpdatedAt(OffsetDateTime.now());
        settings.setUpdatedBy(admin.getId());
        return toSettingsDto(contactSettingsRepository.save(settings));
    }

    @Transactional
    public void submit(ContactMessageRequestDto dto) {
        ContactMessage msg = new ContactMessage();
        msg.setSubject(dto.subject());
        msg.setReplyEmail(dto.replyEmail());
        msg.setMessage(dto.message());
        msg.setSubmittedAt(OffsetDateTime.now());
        contactMessageRepository.save(msg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ContactMessagePageDto listMessages(int page) {
        Page<ContactMessage> result = contactMessageRepository.findAllByOrderBySubmittedAtDesc(
                PageRequest.of(page, PAGE_SIZE));
        return new ContactMessagePageDto(
                result.getContent().stream().map(this::toSummaryDto).toList(),
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ContactMessageSummaryDto markRead(UUID id) {
        User admin = resolveCurrentUser();
        ContactMessage msg = contactMessageRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Message not found: " + id));
        if (msg.getReadAt() == null) {
            msg.setReadAt(OffsetDateTime.now());
            msg.setReadBy(admin.getId());
        }
        return toSummaryDto(contactMessageRepository.save(msg));
    }

    private ContactSettingsDto toSettingsDto(ContactSettings s) {
        return new ContactSettingsDto(s.getPageDescription(), s.getContactEmail(), s.getContactPhone());
    }

    private ContactMessageSummaryDto toSummaryDto(ContactMessage m) {
        return new ContactMessageSummaryDto(
                m.getId(), m.getSubject(), m.getReplyEmail(),
                m.getMessage(), m.getSubmittedAt(), m.getReadAt()
        );
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getPrincipal() instanceof UserDetails ud
                ? ud.getUsername()
                : auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}
