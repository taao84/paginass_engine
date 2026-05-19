package com.paginass.controller;

import com.paginass.dto.AppNewsAdminDto;
import com.paginass.dto.AppNewsAdminPageDto;
import com.paginass.dto.CreateAppNewsRequestDto;
import com.paginass.dto.UpdateAppNewsRequestDto;
import com.paginass.service.AppNewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final AppNewsService appNewsService;

    @GetMapping
    public ResponseEntity<AppNewsAdminPageDto> getAll(
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(appNewsService.getAll(page));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AppNewsAdminDto create(
            @Valid @RequestPart("data") CreateAppNewsRequestDto request,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        return appNewsService.create(request, image);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AppNewsAdminDto> update(
            @PathVariable UUID id,
            @Valid @RequestPart("data") UpdateAppNewsRequestDto request,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(appNewsService.update(id, request, image));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        appNewsService.delete(id);
    }
}
