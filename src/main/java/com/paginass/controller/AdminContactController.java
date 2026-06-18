package com.paginass.controller;

import com.paginass.dto.ContactMessagePageDto;
import com.paginass.dto.ContactMessageSummaryDto;
import com.paginass.dto.ContactSettingsDto;
import com.paginass.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminContactController {

    private final ContactService contactService;

    @GetMapping("/contact-settings")
    public ContactSettingsDto getSettings() {
        return contactService.getSettings();
    }

    @PutMapping("/contact-settings")
    public ContactSettingsDto updateSettings(@RequestBody ContactSettingsDto dto) {
        return contactService.updateSettings(dto);
    }

    @GetMapping("/contact-messages")
    public ContactMessagePageDto listMessages(@RequestParam(defaultValue = "0") int page) {
        return contactService.listMessages(page);
    }

    @PatchMapping("/contact-messages/{id}/read")
    public ContactMessageSummaryDto markRead(@PathVariable UUID id) {
        return contactService.markRead(id);
    }
}
