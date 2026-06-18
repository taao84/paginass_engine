package com.paginass.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paginass.config.FeatureKey;
import com.paginass.config.StorageConstants;
import com.paginass.dto.AdminBusinessPageDto;
import com.paginass.dto.AdminBusinessSummaryDto;
import com.paginass.dto.BusinessMapMarkerDto;
import com.paginass.dto.BusinessMiniSiteDto;
import com.paginass.dto.BusinessProfileDetailDto;
import com.paginass.dto.BusinessProfileEditDto;
import com.paginass.dto.BusinessProfileSummaryDto;
import com.paginass.dto.BusinessProfileUpdateRequestDto;
import com.paginass.dto.ContributorBusinessCreateRequestDto;
import com.paginass.dto.DirectoryEntryDto;
import com.paginass.dto.DirectoryPageDto;
import com.paginass.dto.TagCloudItemDto;
import com.paginass.dto.SearchResponseDto;
import com.paginass.entity.BusinessProfile;
import com.paginass.entity.BusinessProfileEdit;
import com.paginass.entity.BusinessType;
import com.paginass.entity.ClaimStatus;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.BusinessClaimRepository;
import com.paginass.repository.BusinessLocationProjection;
import com.paginass.repository.BusinessProfileEditRepository;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.BusinessProfileSearchProjection;
import com.paginass.repository.DirectoryEntryProjection;
import com.paginass.repository.TagCountProjection;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import com.paginass.util.TagNormalizer;
import java.util.Arrays;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
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
    private static final int ADMIN_PAGE_SIZE = 20;
    private static final long MAX_IMAGE_BYTES = StorageConstants.MAX_BUSINESS_IMAGE_BYTES;

    private final BusinessProfileRepository businessProfileRepository;
    private final BusinessClaimRepository businessClaimRepository;
    private final BusinessProfileEditRepository businessProfileEditRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final LocalAuthService localAuthService;
    private final AppFeatureService appFeatureService;
    private final BusinessLikeService businessLikeService;

    @Transactional(readOnly = true)
    public SearchResponseDto search(String query, int page) {
        String prefixQuery = buildPrefixQuery(query);
        if (prefixQuery.isBlank()) {
            return new SearchResponseDto(Collections.emptyList(), page, 0, 0);
        }
        int offset = page * PAGE_SIZE;

        List<BusinessProfileSearchProjection> rows;
        long total;
        if (appFeatureService.isEnabled(FeatureKey.TIERED_SEARCH)) {
            rows  = businessProfileRepository.searchBusinessProfilesTiered(query, prefixQuery, PAGE_SIZE, offset);
            total = businessProfileRepository.countSearchResultsTiered(query, prefixQuery);
        } else {
            rows  = businessProfileRepository.searchBusinessProfiles(query, prefixQuery, PAGE_SIZE, offset);
            total = businessProfileRepository.countSearchResults(query, prefixQuery);
        }

        List<BusinessProfileSummaryDto> results = rows.stream()
                .map(this::mapRow)
                .toList();

        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        return new SearchResponseDto(results, page, totalPages, total);
    }

    @Transactional(readOnly = true)
    public DirectoryPageDto getDirectory(int page) {
        int offset = page * DIRECTORY_PAGE_SIZE;

        List<DirectoryEntryProjection> rows = businessProfileRepository.findAllForDirectory(DIRECTORY_PAGE_SIZE, offset);
        long total = businessProfileRepository.countAllForDirectory();

        List<DirectoryEntryDto> content = rows.stream()
                .map(p -> new DirectoryEntryDto(p.getName(), p.getSlug(), p.getBizType()))
                .toList();

        int totalPages = (int) Math.ceil((double) total / DIRECTORY_PAGE_SIZE);
        return new DirectoryPageDto(content, page, totalPages, total);
    }

    @Transactional(readOnly = true)
    public DirectoryPageDto getDirectoryByLetter(String letter, int page) {
        int offset = page * DIRECTORY_PAGE_SIZE;
        List<DirectoryEntryProjection> rows;
        long total;
        if ("#".equals(letter)) {
            rows = businessProfileRepository.findByNonAlphaStart(DIRECTORY_PAGE_SIZE, offset);
            total = businessProfileRepository.countByNonAlphaStart();
        } else {
            String normalizedLetter = letter.substring(0, 1).toUpperCase();
            rows = businessProfileRepository.findByFirstLetter(normalizedLetter, DIRECTORY_PAGE_SIZE, offset);
            total = businessProfileRepository.countByFirstLetter(normalizedLetter);
        }
        List<DirectoryEntryDto> content = rows.stream()
                .map(p -> new DirectoryEntryDto(p.getName(), p.getSlug(), p.getBizType()))
                .toList();
        int totalPages = (int) Math.ceil((double) total / DIRECTORY_PAGE_SIZE);
        return new DirectoryPageDto(content, page, totalPages, total);
    }

    @Transactional(readOnly = true)
    public DirectoryPageDto getDirectoryByTag(String tag, int page) {
        int offset = page * DIRECTORY_PAGE_SIZE;
        List<DirectoryEntryProjection> rows = businessProfileRepository.findByTag(tag, DIRECTORY_PAGE_SIZE, offset);
        long total = businessProfileRepository.countByTag(tag);
        List<DirectoryEntryDto> content = rows.stream()
                .map(p -> new DirectoryEntryDto(p.getName(), p.getSlug(), p.getBizType()))
                .toList();
        int totalPages = (int) Math.ceil((double) total / DIRECTORY_PAGE_SIZE);
        return new DirectoryPageDto(content, page, totalPages, total);
    }

    @Transactional(readOnly = true)
    public List<BusinessMapMarkerDto> getBusinessLocations() {

        return businessProfileRepository.findAllWithLocation().stream()
                .map(p -> new BusinessMapMarkerDto(
                        UUID.fromString(p.getId()),
                        p.getName(),
                        p.getSlug(),
                        p.getLatitude(),
                        p.getLongitude(),
                        p.getBizType()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagCloudItemDto> getDirectoryTags() {
        return businessProfileRepository.findAllTagsWithCount().stream()
                .map(p -> new TagCloudItemDto(p.getTag(), p.getCnt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasOwnedBusiness(java.util.UUID userId) {
        return businessProfileRepository.existsByOwnerIdAndIsClaimedTrue(userId);
    }

    @Transactional
    public BusinessProfileDetailDto createOwnedBusiness(ContributorBusinessCreateRequestDto request) {
        User user = resolveCurrentUser();

        BusinessType type;
        try {
            type = BusinessType.valueOf(request.bizType());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bizType: " + request.bizType());
        }
        if (type == BusinessType.BRICK_AND_MORTAR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BRICK_AND_MORTAR businesses cannot be self-registered");
        }

        if (businessProfileRepository.existsByOwnerIdAndIsClaimedTrue(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already own a business");
        }

        Map<String, String> contactInfo = new HashMap<>();
        if (request.phones() != null) contactInfo.put("phones", request.phones());
        if (request.contactHours() != null) contactInfo.put("hours", request.contactHours());
        if (request.address() != null) contactInfo.put("address", request.address());
        if (request.whatsapp() != null && !request.whatsapp().isBlank()) contactInfo.put("whatsapp", request.whatsapp());
        if (request.email() != null && !request.email().isBlank()) contactInfo.put("email", request.email());
        if (request.website() != null && !request.website().isBlank()) contactInfo.put("website", request.website());
        if (request.instagram() != null && !request.instagram().isBlank()) contactInfo.put("instagram", request.instagram());

        BusinessProfile profile = new BusinessProfile();
        profile.setOwnerId(user.getId());
        profile.setCreatedBy(user.getId());
        profile.setName(request.name());
        profile.setSlug(localAuthService.generateSlug(request.name()));
        profile.setBizType(type);
        profile.setDescription(request.description());
        profile.setContactInfo(contactInfo);
        profile.setTags(normalizeTags(request.services()));
        profile.setDeliversToHome(Boolean.TRUE.equals(request.deliversToHome()));
        profile.setLatitude(request.latitude());
        profile.setLongitude(request.longitude());
        profile.setClaimed(true);

        return toDetailDto(businessProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public List<BusinessProfileDetailDto> getProfilesByOwner() {
        User user = resolveCurrentUser();
        return businessProfileRepository.findByOwnerIdAndIsClaimedTrueAndIsActiveTrue(user.getId())
                .stream()
                .map(this::toDetailDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BusinessProfileDetailDto getProfileById(UUID profileId) {
        BusinessProfile profile = businessProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business profile not found"));
        return toDetailDto(profile);
    }

    @Transactional(readOnly = true)
    public AdminBusinessPageDto listBusinessesForAdmin(String search, int page) {
        Specification<BusinessProfile> spec = (root, query, cb) -> {
            if (search != null && !search.isBlank()) {
                return cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%");
            }
            return null;
        };
        Page<BusinessProfile> result = businessProfileRepository.findAll(
                spec, PageRequest.of(page, ADMIN_PAGE_SIZE, Sort.by("name").ascending()));
        List<AdminBusinessSummaryDto> content = result.getContent().stream()
                .map(b -> new AdminBusinessSummaryDto(
                        b.getId(), b.getName(), b.getSlug(),
                        b.getBizType().name(), b.isClaimed(), b.isActive(), b.getCreatedAt()))
                .toList();
        return new AdminBusinessPageDto(content, page, result.getTotalPages(), result.getTotalElements());
    }

    @PreAuthorize("""
            hasRole('ADMIN') or
            @bizAuth.isOwnerOfClaimed(#profileId) or
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
        if (request.whatsapp() != null && !request.whatsapp().isBlank()) contactInfo.put("whatsapp", request.whatsapp());
        if (request.email() != null && !request.email().isBlank()) contactInfo.put("email", request.email());
        if (request.website() != null && !request.website().isBlank()) contactInfo.put("website", request.website());
        if (request.instagram() != null && !request.instagram().isBlank()) contactInfo.put("instagram", request.instagram());
        profile.setContactInfo(contactInfo);

        profile.setTags(normalizeTags(request.services()));
        profile.setDeliversToHome(Boolean.TRUE.equals(request.deliversToHome()));
        profile.setLatitude(request.latitude());
        profile.setLongitude(request.longitude());

        if (request.bizType() != null && user.getRole() == UserRole.ADMIN) {
            try {
                profile.setBizType(BusinessType.valueOf(request.bizType()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bizType: " + request.bizType());
            }
        }

        BusinessProfile saved = businessProfileRepository.save(profile);
        recordEditSnapshot(saved, user);
        return toDetailDto(saved);
    }

    @Transactional(readOnly = true)
    public BusinessMiniSiteDto getMiniSiteBySlug(String slug) {
        BusinessProfile profile = businessProfileRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mini site not found"));
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
            @bizAuth.isOwnerOfClaimed(#businessId) or
            (hasRole('EDITOR') and @bizAuth.isUnclaimed(#businessId))
            """)
    public BusinessProfileDetailDto uploadBannerImage(UUID businessId, MultipartFile file) {
        validateImageFile(file);
        BusinessProfile profile = findProfileOrThrow(businessId);
        deleteStoredFileIfPresent(profile.getBannerUrl());
        try {
            String extension = getExtension(file.getOriginalFilename());
            String subPath = "businesses/" + businessId + "/banner/" + UUID.randomUUID() + extension;
            storageService.saveFile(subPath, file.getInputStream(), file.getContentType());
            profile.setBannerUrl(subPath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save banner image");
        }
        return toDetailDto(businessProfileRepository.save(profile));
    }

    @PreAuthorize("""
            hasRole('ADMIN') or
            @bizAuth.isOwnerOfClaimed(#businessId) or
            (hasRole('EDITOR') and @bizAuth.isUnclaimed(#businessId))
            """)
    public BusinessProfileDetailDto deleteBannerImage(UUID businessId) {
        BusinessProfile profile = findProfileOrThrow(businessId);
        if (profile.getBannerUrl() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No banner image set");
        }
        deleteStoredFileIfPresent(profile.getBannerUrl());
        profile.setBannerUrl(null);
        return toDetailDto(businessProfileRepository.save(profile));
    }

    @PreAuthorize("""
            hasRole('ADMIN') or
            @bizAuth.isOwnerOfClaimed(#businessId) or
            (hasRole('EDITOR') and @bizAuth.isUnclaimed(#businessId))
            """)
    public BusinessProfileDetailDto uploadThumbnailImage(UUID businessId, MultipartFile file) {
        validateImageFile(file);
        BusinessProfile profile = findProfileOrThrow(businessId);
        deleteStoredFileIfPresent(profile.getThumbnailUrl());
        try {
            String extension = getExtension(file.getOriginalFilename());
            String subPath = "businesses/" + businessId + "/thumbnail/" + UUID.randomUUID() + extension;
            storageService.saveFile(subPath, file.getInputStream(), file.getContentType());
            profile.setThumbnailUrl(subPath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save thumbnail image");
        }
        return toDetailDto(businessProfileRepository.save(profile));
    }

    @PreAuthorize("""
            hasRole('ADMIN') or
            @bizAuth.isOwnerOfClaimed(#businessId) or
            (hasRole('EDITOR') and @bizAuth.isUnclaimed(#businessId))
            """)
    public BusinessProfileDetailDto deleteThumbnailImage(UUID businessId) {
        BusinessProfile profile = findProfileOrThrow(businessId);
        if (profile.getThumbnailUrl() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No thumbnail image set");
        }
        deleteStoredFileIfPresent(profile.getThumbnailUrl());
        profile.setThumbnailUrl(null);
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
        snapshot.put("bannerUrl", profile.getBannerUrl());
        snapshot.put("thumbnailUrl", profile.getThumbnailUrl());
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
        if (snapshot.containsKey("bannerUrl")) {
            profile.setBannerUrl(snapshot.get("bannerUrl") instanceof String s ? s : null);
        }
        if (snapshot.containsKey("thumbnailUrl")) {
            profile.setThumbnailUrl(snapshot.get("thumbnailUrl") instanceof String s ? s : null);
        }
        if (snapshot.get("deliversToHome") instanceof Boolean dth) profile.setDeliversToHome(dth);
    }

    private BusinessMiniSiteDto toMiniSiteDto(BusinessProfile profile) {
        boolean hasPendingClaim = businessClaimRepository
                .existsByBusinessIdAndStatus(profile.getId(), ClaimStatus.PENDING);
        long likeCount = businessLikeService.getLikeCount(profile.getId());
        boolean isOwner = resolveIsOwner(profile.getOwnerId());
        return new BusinessMiniSiteDto(
                profile.getId(),
                profile.getName(),
                profile.getDescription(),
                profile.getBizType() != null ? profile.getBizType().name() : null,
                profile.getContactInfo(),
                toPublicUrl(profile.getBannerUrl()),
                profile.getTags() != null ? profile.getTags() : List.of(),
                profile.isClaimed(),
                hasPendingClaim,
                profile.isDeliversToHome(),
                profile.getLatitude(),
                profile.getLongitude(),
                likeCount,
                isOwner
        );
    }

    private boolean resolveIsOwner(UUID ownerId) {
        if (ownerId == null) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        String email = auth.getPrincipal() instanceof UserDetails ud
                ? ud.getUsername() : auth.getName();
        return userRepository.findByEmail(email)
                .map(u -> ownerId.equals(u.getId()))
                .orElse(false);
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
                toPublicUrl(p.getBannerUrl()),
                toPublicUrl(p.getThumbnailUrl()),
                p.isActive(),
                p.isClaimed(),
                p.getCreatedBy(),
                p.isDeliversToHome(),
                p.getLatitude(),
                p.getLongitude(),
                resolveIsOwner(p.getOwnerId())
        );
    }

    private String toPublicUrl(String subPath) {
        if (subPath == null) return null;
        return storageService.toPublicUrl(subPath);
    }

    private void validateImageFile(MultipartFile file) {
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must not exceed 4 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be an image");
        }
    }

    private void deleteStoredFileIfPresent(String subPath) {
        if (subPath == null) return;
        try {
            storageService.deleteFile(subPath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to remove existing image");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }

    private List<String> normalizeTags(List<String> tags) {
        return TagNormalizer.normalize(tags);
    }

    private String buildPrefixQuery(String input) {
        return Arrays.stream(input.split("\\s+"))
                .map(token -> token.replaceAll("[!&|():*'\\\\<>]", ""))
                .filter(token -> !token.isEmpty())
                .map(token -> token + ":*")
                .collect(Collectors.joining(" & "));
    }

    private BusinessProfileSummaryDto mapRow(BusinessProfileSearchProjection p) {
        try {
            List<String> tags = p.getTags() != null
                    ? objectMapper.readValue(p.getTags(), new TypeReference<>() {})
                    : Collections.emptyList();

            Map<String, String> contactInfo = p.getContactInfo() != null
                    ? objectMapper.readValue(p.getContactInfo(), new TypeReference<>() {})
                    : Collections.emptyMap();

            return new BusinessProfileSummaryDto(
                    UUID.fromString(p.getId()),
                    p.getName(),
                    p.getSlug(),
                    p.getBizType(),
                    p.getDescription(),
                    tags,
                    contactInfo,
                    p.getScore() != null ? p.getScore() : 0.0,
                    Boolean.TRUE.equals(p.getIsClaimed()),
                    Boolean.TRUE.equals(p.getDeliversToHome()),
                    toPublicUrl(p.getThumbnailUrl()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to map search result row", e);
        }
    }
}
