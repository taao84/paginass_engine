package com.paginass.controller;

import com.paginass.dto.ContactMessageRequestDto;
import com.paginass.dto.ContactSettingsDto;
import com.paginass.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicContactController {

    private final ContactService contactService;

    @GetMapping("/contact-settings")
    public ContactSettingsDto getSettings() {
        return contactService.getSettings();
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public void submit(@Valid @RequestBody ContactMessageRequestDto request) {
        contactService.submit(request);
    }
}
