package com.paginass.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paginass.dto.BusinessMiniSiteDto;
import com.paginass.dto.BusinessProfileDetailDto;
import com.paginass.dto.BusinessProfileEditDto;
import com.paginass.dto.BusinessProfileSummaryDto;
import com.paginass.dto.BusinessProfileUpdateRequestDto;
import com.paginass.dto.DirectoryEntryDto;
import com.paginass.dto.DirectoryPageDto;
import com.paginass.dto.SearchResponseDto;
import com.paginass.entity.BusinessProfile;
import com.paginass.entity.BusinessProfileEdit;
import com.paginass.entity.ClaimStatus;
import com.paginass.entity.User;
import com.paginass.repository.BusinessClaimRepository;
import com.paginass.repository.BusinessProfileEditRepository;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessProfileService {

    private static final int PAGE_SIZE = 10;
    private static final int DIRECTORY_PAGE_SIZE = 50;
    private static final int MAX_IMAGES = 5;

    private final BusinessProfileRepository businessProfileRepository;
    private final BusinessClaimRepository businessClaimRepository;
    private final BusinessProfileEditRepository businessProfileEditRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public SearchResponseDto search(String query, int page) {
        int offset = page * PAGE_SIZE;

        List<Object[]> rows = businessProfileRepository.searchBusinessProfiles(query, PAGE_SIZE, offset);
        long total = businessProfileRepository.countSearchResults(query);

        List<BusinessProfileSummaryDto> results = rows.stream()
                .map(this::mapRow)
                .toList();

        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        return new SearchResponseDto(results, page, totalPages, total);
    }

    @Transactional(readOnly = true)
    public DirectoryPageDto getDirectory(int page) {
        int offset = page * DIRECTORY_PAGE_SIZE;

        List<Object[]> rows = businessProfileRepository.findAllForDirectory(DIRECTORY_PAGE_SIZE, offset);
        long total = businessProfileRepository.countAllForDirectory();

        List<DirectoryEntryDto> content = rows.stream()
                .map(row -> new DirectoryEntryDto((String) row[0], (String) row[1], (String) row[2]))
                .toList();

        int totalPages = (int) Math.ceil((double) total / DIRECTORY_PAGE_SIZE);
        return new DirectoryPageDto(content, page, totalPages, total);
    }

    @Transactional(readOnly = true)
    public List<BusinessProfileDetailDto> getProfilesByOwner() {
        User user = resolveCurrentUser();
        return businessProfileRepository.findByOwnerIdAndIsClaimedTrueAndIsActiveTrue(user.getId())
                .stream()
                .map(this::toDetailDto)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN') or @bizAuth.isOwnerOfClaimed(#profileId)")
    @Transactional(readOnly = true)
    public BusinessProfileDetailDto getProfileByOwner(UUID profileId) {
        return toDetailDto(findProfileOrThrow(profileId));
    }

    @Transactional(readOnly = true)
    public BusinessProfileDetailDto getProfileById(UUID profileId) {
        BusinessProfile profile = businessProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business profile not found"));
        return toDetailDto(profile);
    }

    @PreAuthorize("""
            hasRole('ADMIN') or
            (hasRole('OWNER') and @bizAuth.isOwnerOfClaimed(#profileId)) or
            (hasRole('EDITOR') and @bizAuth.isUnclaimed(#profileId))
            """)
    @Transactional
    public BusinessProfileDetailDto updateProfile(UUID profileId, BusinessProfileUpdateRequestDto request) {
        User user = resolveCurrentUser();
        BusinessProfile profile = findProfileOrThrow(profileId);

        profile.setName(request.name());
        profile.setDescription(request.description());

        Map<String, String> contactInfo = new HashMap<>();
        if (request.phones() != null) contactInfo.put("phones", request.phones());
        if (request.contactHours() != null) contactInfo.put("hours", request.contactHours());
        if (request.address() != null) contactInfo.put("address", request.address());
        profile.setContactInfo(contactInfo);

        profile.setTags(parseCsv(request.services()));
        profile.setDeliversToHome(Boolean.TRUE.equals(request.deliversToHome()));

        BusinessProfile saved = businessProfileRepository.save(profile);
        recordEditSnapshot(saved, user);
        return toDetailDto(saved);
    }

    @PreAuthorize("""
            hasRole('ADMIN') or
            (hasRole('OWNER') and @bizAuth.isOwnerOfClaimed(#businessId))
            """)
    public BusinessProfileDetailDto toggleMiniSite(UUID businessId) {
        BusinessProfile profile = findProfileOrThrow(businessId);
        profile.setMiniSiteEnabled(!profile.isMiniSiteEnabled());
        return toDetailDto(businessProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public BusinessMiniSiteDto getMiniSiteBySlug(String slug) {
        BusinessProfile profile = businessProfileRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mini site not found"));
        if (!profile.isMiniSiteEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mini site not found");
        }
        return toMiniSiteDto(profile);
    }

    @Transactional(readOnly = true)
    public BusinessMiniSiteDto getPublicBusinessBySlug(String slug) {
        BusinessProfile profile = businessProfileRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
        return toMiniSiteDto(profile);
    }

    @PreAuthorize("""
            hasRole('ADMIN') or
            (hasRole('OWNER') and @bizAuth.isOwnerOfClaimed(#businessId)) or
            (hasRole('EDITOR') and @bizAuth.isUnclaimed(#businessId))
            """)
    public BusinessProfileDetailDto uploadImage(UUID businessId, MultipartFile file) {
        BusinessProfile profile = findProfileOrThrow(businessId);

        List<String> currentPaths = profile.getImageUrls() == null
                ? new ArrayList<>() : new ArrayList<>(profile.getImageUrls());

        if (currentPaths.size() >= MAX_IMAGES) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Maximum of " + MAX_IMAGES + " images allowed per business");
        }

        try {
            String extension = getExtension(file.getOriginalFilename());
            String subPath = "businesses/" + businessId + "/" + UUID.randomUUID() + extension;
            storageService.saveFile(subPath, file.getInputStream());
            currentPaths.add(subPath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save image");
        }

        profile.setImageUrls(currentPaths);
        return toDetailDto(businessProfileRepository.save(profile));
    }

    @PreAuthorize("""
            hasRole('ADMIN') or
            (hasRole('OWNER') and @bizAuth.isOwnerOfClaimed(#businessId)) or
            (hasRole('EDITOR') and @bizAuth.isUnclaimed(#businessId))
            """)
    public BusinessProfileDetailDto reorderImages(UUID businessId, List<String> orderedUrls) {
        BusinessProfile profile = findProfileOrThrow(businessId);

        List<String> currentPaths = profile.getImageUrls() != null ? profile.getImageUrls() : List.of();
        List<String> orderedPaths = orderedUrls.stream()
                .map(url -> currentPaths.stream()
                        .filter(p -> storageService.toPublicUrl(p).equals(url))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image URL")))
                .toList();
        if (orderedPaths.size() != currentPaths.size()
                || !new HashSet<>(orderedPaths).equals(new HashSet<>(currentPaths))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image list does not match stored images");
        }

        profile.setImageUrls(orderedPaths);
        return toDetailDto(businessProfileRepository.save(profile));
    }

    @PreAuthorize("""
            hasRole('ADMIN') or
            (hasRole('OWNER') and @bizAuth.isOwnerOfClaimed(#businessId)) or
            (hasRole('EDITOR') and @bizAuth.isUnclaimed(#businessId))
            """)
    public BusinessProfileDetailDto deleteImage(UUID businessId, int imageIndex) {
        BusinessProfile profile = findProfileOrThrow(businessId);

        List<String> currentPaths = new ArrayList<>(profile.getImageUrls());
        if (imageIndex < 0 || imageIndex >= currentPaths.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image index");
        }

        String subPath = currentPaths.remove(imageIndex);
        try {
            storageService.deleteFile(subPath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete image");
        }

        profile.setImageUrls(currentPaths);
        return toDetailDto(businessProfileRepository.save(profile));
    }

    // --- Admin: edit history & restore ---

    @Transactional(readOnly = true)
    public List<BusinessProfileEditDto> getEditHistory(UUID profileId) {
        findProfileOrThrow(profileId);
        List<BusinessProfileEdit> edits = businessProfileEditRepository
                .findByBusinessProfileIdOrderByEditedAtDesc(profileId);

        Set<UUID> editorIds = edits.stream().map(BusinessProfileEdit::getEditedBy).collect(Collectors.toSet());
        Map<UUID, String> nameById = userRepository.findAllById(editorIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> u.getFullName() != null ? u.getFullName() : u.getEmail()));

        return edits.stream()
                .map(e -> new BusinessProfileEditDto(
                        e.getId(),
                        e.getBusinessProfileId(),
                        e.getEditedBy(),
                        nameById.getOrDefault(e.getEditedBy(), "Unknown"),
                        e.getEditedAt(),
                        e.getSnapshot()))
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public BusinessProfileDetailDto restoreFromEdit(UUID profileId, UUID editId) {
        User admin = resolveCurrentUser();
        BusinessProfile profile = findProfileOrThrow(profileId);
        BusinessProfileEdit edit = businessProfileEditRepository.findById(editId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Edit record not found"));

        if (!edit.getBusinessProfileId().equals(profileId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Edit does not belong to this business");
        }

        applySnapshot(profile, edit.getSnapshot());
        BusinessProfile saved = businessProfileRepository.save(profile);
        recordEditSnapshot(saved, admin);
        return toDetailDto(saved);
    }

    // --- Private helpers ---

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getPrincipal() instanceof UserDetails ud
                ? ud.getUsername()
                : auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    private void recordEditSnapshot(BusinessProfile profile, User editor) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("name", profile.getName());
        snapshot.put("description", profile.getDescription());
        snapshot.put("bizType", profile.getBizType() != null ? profile.getBizType().name() : null);
        snapshot.put("contactInfo", profile.getContactInfo());
        snapshot.put("tags", profile.getTags());
        snapshot.put("imageUrls", profile.getImageUrls());
        snapshot.put("deliversToHome", profile.isDeliversToHome());

        BusinessProfileEdit edit = new BusinessProfileEdit();
        edit.setBusinessProfileId(profile.getId());
        edit.setEditedBy(editor.getId());
        edit.setSnapshot(snapshot);
        edit.setEditedAt(OffsetDateTime.now(ZoneOffset.UTC));
        businessProfileEditRepository.save(edit);
    }

    private void applySnapshot(BusinessProfile profile, Map<String, Object> snapshot) {
        if (snapshot.get("name") instanceof String name) profile.setName(name);
        if (snapshot.get("description") instanceof String desc) profile.setDescription(desc);
        if (snapshot.get("contactInfo") instanceof Map<?, ?> ci) {
            profile.setContactInfo(ci.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> (String) e.getKey(),
                            e -> String.valueOf(e.getValue()))));
        }
        if (snapshot.get("tags") instanceof List<?> tags) {
            profile.setTags(tags.stream().map(Object::toString).toList());
        }
        if (snapshot.get("imageUrls") instanceof List<?> urls) {
            profile.setImageUrls(urls.stream().map(Object::toString).toList());
        }
        if (snapshot.get("deliversToHome") instanceof Boolean dth) profile.setDeliversToHome(dth);
    }

    private BusinessMiniSiteDto toMiniSiteDto(BusinessProfile profile) {
        boolean hasPendingClaim = businessClaimRepository
                .existsByBusinessIdAndStatus(profile.getId(), ClaimStatus.PENDING);
        return new BusinessMiniSiteDto(
                profile.getId(),
                profile.getName(),
                profile.getDescription(),
                profile.getContactInfo(),
                toPublicUrls(profile.getImageUrls()),
                profile.getTags() != null ? profile.getTags() : List.of(),
                profile.isMiniSiteEnabled(),
                profile.isClaimed(),
                hasPendingClaim
        );
    }

    private BusinessProfile findProfileOrThrow(UUID profileId) {
        return businessProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business profile not found"));
    }

    private BusinessProfileDetailDto toDetailDto(BusinessProfile p) {
        return new BusinessProfileDetailDto(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getBizType().name(),
                p.getDescription(),
                p.getContactInfo(),
                p.getTags(),
                toPublicUrls(p.getImageUrls()),
                p.isMiniSiteEnabled(),
                p.isActive(),
                p.isClaimed(),
                p.getCreatedBy(),
                p.isDeliversToHome()
        );
    }

    private List<String> toPublicUrls(List<String> subPaths) {
        if (subPaths == null) return List.of();
        return subPaths.stream()
                .map(storageService::toPublicUrl)
                .toList();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }

    private List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // Column positions must match the SELECT order in BusinessProfileRepository.searchBusinessProfiles:
    // [0] id, [1] name, [2] slug, [3] biz_type, [4] description, [5] tags, [6] contact_info, [7] score, [8] mini_site_enabled, [9] is_claimed, [10] delivers_to_home
    private BusinessProfileSummaryDto mapRow(Object[] row) {
        try {
            UUID id = UUID.fromString((String) row[0]);
            String name        = (String) row[1];
            String slug        = (String) row[2];
            String bizType     = (String) row[3];
            String description = (String) row[4];

            List<String> tags = row[5] != null
                    ? objectMapper.readValue((String) row[5], new TypeReference<>() {})
                    : Collections.emptyList();

            Map<String, String> contactInfo = row[6] != null
                    ? objectMapper.readValue((String) row[6], new TypeReference<>() {})
                    : Collections.emptyMap();

            double score = row[7] != null ? ((Number) row[7]).doubleValue() : 0.0;

            boolean miniSiteEnabled = row[8] != null && (Boolean) row[8];

            boolean isClaimed = row[9] != null && (Boolean) row[9];

            boolean deliversToHome = row[10] != null && (Boolean) row[10];

            return new BusinessProfileSummaryDto(id, name, slug, bizType, description, tags, contactInfo, score, miniSiteEnabled, isClaimed, deliversToHome);
        } catch (Exception e) {
            throw new RuntimeException("Failed to map search result row", e);
        }
    }
}
