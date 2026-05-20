package com.paginass.service;

import com.paginass.dto.AdvertisementRequestDto;
import com.paginass.dto.AdvertisementResponseDto;
import com.paginass.dto.AdvertisementSummaryDto;
import com.paginass.entity.AdCategory;
import com.paginass.entity.Advertisement;
import com.paginass.entity.User;
import com.paginass.repository.AdvertisementRepository;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private static final int AD_EXPIRY_DAYS = 30;
    private static final int MAX_SALE_IMAGES = 3;
    private static final int MAX_EVENT_IMAGES = 1;

    private final AdvertisementRepository advertisementRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    // Self-injection so that calls to @Transactional methods within this class
    // go through the Spring proxy rather than bypassing it via `this`.
    @Lazy
    @Autowired
    private AdvertisementService self;

    @Transactional
    public AdvertisementResponseDto createAdvertisement(AdvertisementRequestDto request) {
        User owner = resolveCurrentUser();
        Advertisement ad = new Advertisement();
        ad.setOwnerId(owner.getId());
        ad.setCategory(request.category());
        ad.setTitle(request.title());
        ad.setContent(request.content());
        ad.setPrice(request.price());
        ad.setEventDate(request.eventDate());
        ad.setTags(request.tags() != null ? request.tags() : new ArrayList<>());
        if (request.category() == AdCategory.EVENT) {
            if (request.eventDate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EVENT advertisements require an event date");
            }
            ad.setExpiresAt(request.eventDate().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS));
        } else {
            ad.setExpiresAt(Instant.now().plus(AD_EXPIRY_DAYS, ChronoUnit.DAYS));
        }
        advertisementRepository.save(ad);
        return toResponseDto(ad, owner);
    }

    @PreAuthorize("@adAuth.isOwner(#adId)")
    public AdvertisementResponseDto addImages(UUID adId, List<MultipartFile> files) {
        Advertisement ad = loadAdOrThrow(adId);
        validateImageCount(ad.getCategory(), ad.getImageUrls().size(), files.size());
        List<String> uploadedPaths = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                String subPath = "ads/" + adId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
                storageService.saveFile(subPath, file.getInputStream(), file.getContentType());
                uploadedPaths.add(subPath);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Image upload failed");
        }
        return self.persistImages(adId, uploadedPaths);
    }

    @Transactional
    public AdvertisementResponseDto persistImages(UUID adId, List<String> paths) {
        Advertisement ad = loadAdOrThrow(adId);
        ad.getImageUrls().addAll(paths);
        return toResponseDto(ad, loadOwnerOrThrow(ad.getOwnerId()));
    }

    @Transactional(readOnly = true)
    public List<AdvertisementSummaryDto> getActiveAdvertisements(AdCategory category, String search) {
        List<Advertisement> ads;
        if (search != null && !search.isBlank()) {
            ads = advertisementRepository.searchByKeywordAndActive("%" + search.trim() + "%");
        } else if (category != null) {
            ads = advertisementRepository.findAllByCategoryAndActiveTrueOrderByCreatedAtDesc(category);
        } else {
            ads = advertisementRepository.findAllByActiveTrueOrderByCreatedAtDesc();
        }
        return toSummaryDtos(ads);
    }

    @Transactional(readOnly = true)
    public AdvertisementResponseDto getAdvertisementById(UUID id) {
        Advertisement ad = loadAdOrThrow(id);
        if (!ad.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Advertisement not found");
        }
        return toResponseDto(ad, loadOwnerOrThrow(ad.getOwnerId()));
    }

    @Transactional(readOnly = true)
    public List<AdvertisementSummaryDto> getMyAdvertisements() {
        User current = resolveCurrentUser();
        return advertisementRepository
                .findAllByOwnerIdAndActiveTrueOrderByCreatedAtDesc(current.getId())
                .stream()
                .map(ad -> toSummaryDto(ad, current))
                .toList();
    }

    @PreAuthorize("@adAuth.isOwnerOrAdmin(#id)")
    public void deleteAdvertisement(UUID id) {
        List<String> pathsToDelete = self.deleteAdvertisementRecord(id);
        for (String path : pathsToDelete) {
            try {
                storageService.deleteFile(path);
            } catch (IOException e) {
                // log but continue — record is already deleted
            }
        }
    }

    @Transactional
    public List<String> deleteAdvertisementRecord(UUID id) {
        Advertisement ad = loadAdOrThrow(id);
        List<String> imagePaths = new ArrayList<>(ad.getImageUrls());
        advertisementRepository.delete(ad);
        return imagePaths;
    }

    // --- Private helpers ---

    private List<AdvertisementSummaryDto> toSummaryDtos(List<Advertisement> ads) {
        if (ads.isEmpty()) return List.of();
        Set<UUID> ownerIds = ads.stream().map(Advertisement::getOwnerId).collect(Collectors.toSet());
        Map<UUID, User> ownerMap = userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return ads.stream().map(ad -> toSummaryDto(ad, ownerMap.get(ad.getOwnerId()))).toList();
    }

    private void validateImageCount(AdCategory category, int existing, int incoming) {
        if (category == AdCategory.JOB) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Images are not allowed for JOB advertisements");
        }
        int total = existing + incoming;
        if (category == AdCategory.SALE && total > MAX_SALE_IMAGES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "SALE advertisements allow at most " + MAX_SALE_IMAGES + " images");
        }
        if (category == AdCategory.EVENT && total > MAX_EVENT_IMAGES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "EVENT advertisements allow at most " + MAX_EVENT_IMAGES + " image");
        }
    }

    private Advertisement loadAdOrThrow(UUID id) {
        return advertisementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Advertisement not found"));
    }

    private User loadOwnerOrThrow(UUID ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalStateException("Owner not found: " + ownerId));
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getPrincipal() instanceof UserDetails ud
                ? ud.getUsername()
                : auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    private AdvertisementResponseDto toResponseDto(Advertisement ad, User owner) {
        return new AdvertisementResponseDto(
                ad.getId(),
                ad.getCategory(),
                ad.getTitle(),
                ad.getContent(),
                ad.getPrice(),
                ad.getEventDate(),
                ad.getTags(),
                toPublicUrls(ad.getImageUrls()),
                owner.getFullName(),
                owner.getId(),
                ad.getCreatedAt(),
                ad.getExpiresAt()
        );
    }

    private AdvertisementSummaryDto toSummaryDto(Advertisement ad, User owner) {
        List<String> publicUrls = toPublicUrls(ad.getImageUrls());
        String firstImage = publicUrls.isEmpty() ? null : publicUrls.get(0);
        return new AdvertisementSummaryDto(
                ad.getId(),
                ad.getCategory(),
                ad.getTitle(),
                ad.getPrice(),
                ad.getEventDate(),
                ad.getTags(),
                firstImage,
                ad.getExpiresAt(),
                ad.getCreatedAt(),
                owner.getFullName()
        );
    }

    private List<String> toPublicUrls(List<String> subPaths) {
        if (subPaths == null) return List.of();
        return subPaths.stream().map(storageService::toPublicUrl).toList();
    }
}
