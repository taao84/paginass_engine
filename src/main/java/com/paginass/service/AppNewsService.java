package com.paginass.service;

import com.paginass.config.StorageConstants;
import com.paginass.dto.AppNewsAdminDto;
import com.paginass.dto.AppNewsAdminPageDto;
import com.paginass.dto.AppNewsDto;
import com.paginass.dto.AppNewsPageDto;
import com.paginass.dto.CreateAppNewsRequestDto;
import com.paginass.dto.UpdateAppNewsRequestDto;
import com.paginass.entity.AppNews;
import com.paginass.entity.User;
import com.paginass.repository.AppNewsRepository;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppNewsService {

    private static final long MAX_IMAGE_BYTES = StorageConstants.MAX_NEWS_IMAGE_BYTES;
    private static final int PUBLIC_PAGE_SIZE = 10;
    private static final int ADMIN_PAGE_SIZE  = 20;

    private final AppNewsRepository appNewsRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public AppNewsDto getPublishedById(UUID id) {
        AppNews news = appNewsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News post not found"));
        return toPublicDto(news);
    }

    @Transactional(readOnly = true)
    public AppNewsPageDto getPublished(int page) {
        int offset = page * PUBLIC_PAGE_SIZE;
        List<AppNews> rows = appNewsRepository.findAllPaged(PUBLIC_PAGE_SIZE, offset);
        long total = appNewsRepository.count();
        int totalPages = (int) Math.ceil((double) total / PUBLIC_PAGE_SIZE);
        return new AppNewsPageDto(rows.stream().map(this::toPublicDto).toList(), page, totalPages, total);
    }

    @Transactional(readOnly = true)
    public AppNewsAdminPageDto getAll(int page) {
        int offset = page * ADMIN_PAGE_SIZE;
        List<AppNews> rows = appNewsRepository.findAllPaged(ADMIN_PAGE_SIZE, offset);
        long total = appNewsRepository.count();

        List<UUID> creatorIds = rows.stream().map(AppNews::getCreatedById).distinct().toList();
        Map<UUID, String> nameById = userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(User::getId,
                        u -> u.getFullName() != null ? u.getFullName() : ""));

        int totalPages = (int) Math.ceil((double) total / ADMIN_PAGE_SIZE);
        return new AppNewsAdminPageDto(
                rows.stream().map(n -> toAdminDto(n, nameById.get(n.getCreatedById()))).toList(),
                page, totalPages, total);
    }

    public AppNewsAdminDto create(CreateAppNewsRequestDto request, MultipartFile image) {
        String subPath = saveImageIfPresent(image, null);

        User currentUser = resolveCurrentUser();
        AppNews news = new AppNews();
        news.setTitle(request.title());
        news.setDescription(request.description());
        news.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        news.setCreatedById(currentUser.getId());
        news.setImageUrl(subPath);

        return toAdminDto(appNewsRepository.save(news), currentUser.getFullName());
    }

    public AppNewsAdminDto update(UUID id, UpdateAppNewsRequestDto request, MultipartFile image) {
        AppNews news = findOrThrow(id);

        String existingSubPath = news.getImageUrl();
        String newSubPath;

        if (image != null && !image.isEmpty()) {
            newSubPath = saveImageIfPresent(image, existingSubPath);
        } else if (request.removeImage() && existingSubPath != null) {
            deleteImageQuietly(existingSubPath);
            newSubPath = null;
        } else {
            newSubPath = existingSubPath;
        }

        news.setTitle(request.title());
        news.setDescription(request.description());
        news.setImageUrl(newSubPath);

        String createdByName = userRepository.findById(news.getCreatedById())
                .map(User::getFullName)
                .orElse(null);

        return toAdminDto(appNewsRepository.save(news), createdByName);
    }

    public void delete(UUID id) {
        AppNews news = findOrThrow(id);
        if (news.getImageUrl() != null) {
            deleteImageQuietly(news.getImageUrl());
        }
        appNewsRepository.delete(news);
    }

    private String saveImageIfPresent(MultipartFile image, String oldSubPath) {
        if (image == null || image.isEmpty()) return null;

        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must not exceed 3 MB");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are accepted");
        }

        if (oldSubPath != null) {
            deleteImageQuietly(oldSubPath);
        }

        try {
            String ext = getExtension(image.getOriginalFilename());
            String subPath = "news/" + UUID.randomUUID() + ext;
            storageService.saveFile(subPath, image.getInputStream(), contentType);
            return subPath;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save image");
        }
    }

    private void deleteImageQuietly(String subPath) {
        try {
            storageService.deleteFile(subPath);
        } catch (IOException ignored) {
        }
    }

    private AppNews findOrThrow(UUID id) {
        return appNewsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News post not found"));
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getPrincipal() instanceof UserDetails ud
                ? ud.getUsername()
                : auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }

    private AppNewsDto toPublicDto(AppNews news) {
        return new AppNewsDto(
                news.getId(),
                news.getTitle(),
                news.getDescription(),
                news.getImageUrl() != null ? storageService.toPublicUrl(news.getImageUrl()) : null,
                news.getCreatedAt());
    }

    private AppNewsAdminDto toAdminDto(AppNews news, String createdByName) {
        return new AppNewsAdminDto(
                news.getId(),
                news.getTitle(),
                news.getDescription(),
                news.getImageUrl() != null ? storageService.toPublicUrl(news.getImageUrl()) : null,
                news.getCreatedAt(),
                createdByName);
    }
}
