package com.paginass.service;

import com.paginass.dto.CampaignBusinessDto;
import com.paginass.dto.CampaignCreateRequestDto;
import com.paginass.dto.CampaignDto;
import com.paginass.dto.CampaignStatusUpdateRequestDto;
import com.paginass.dto.PhotoContestDetailsDto;
import com.paginass.entity.Campaign;
import com.paginass.entity.CampaignBusiness;
import com.paginass.entity.CampaignDetails;
import com.paginass.entity.CampaignStatus;
import com.paginass.entity.CampaignType;
import com.paginass.entity.User;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.CampaignBusinessRepository;
import com.paginass.repository.CampaignRepository;
import com.paginass.repository.UserRepository;
import com.paginass.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private static final long MAX_IMAGE_SIZE = 3 * 1024 * 1024;

    private final CampaignRepository campaignRepository;
    private final CampaignBusinessRepository campaignBusinessRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Transactional
    public CampaignDto create(CampaignCreateRequestDto request, MultipartFile image) {
        User admin = resolveCurrentUser();

        Campaign campaign = new Campaign();
        campaign.setName(request.name());
        campaign.setDescription(request.description());
        campaign.setCampaignType(request.campaignType());
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setSlug(generateUniqueSlug(request.name()));
        campaign.setEndDatetime(request.endDatetime());
        campaign.setCreatedBy(admin.getId());
        campaignRepository.save(campaign);

        if (image != null && !image.isEmpty()) {
            String subPath = uploadImage(campaign.getId(), image);
            campaign.setImageSubpath(subPath);
        }

        if (request.campaignType() == CampaignType.PHOTO_CONTEST) {
            CampaignDetails details = new CampaignDetails(
                    HtmlSanitizer.sanitize(request.rules()),
                    HtmlSanitizer.sanitize(request.conditions()),
                    HtmlSanitizer.sanitize(request.awards())
            );
            campaign.setDetails(details);
        }

        return toDto(campaign);
    }

    @Transactional
    public CampaignDto update(UUID id, CampaignCreateRequestDto request, MultipartFile image) {
        Campaign campaign = findCampaignById(id);
        campaign.setName(request.name());
        campaign.setDescription(request.description());
        campaign.setEndDatetime(request.endDatetime());
        campaign.setUpdatedAt(OffsetDateTime.now());

        if (image != null && !image.isEmpty()) {
            if (campaign.getImageSubpath() != null) {
                try { storageService.deleteFile(campaign.getImageSubpath()); } catch (IOException ignored) {}
            }
            campaign.setImageSubpath(uploadImage(campaign.getId(), image));
        }

        if (campaign.getCampaignType() == CampaignType.PHOTO_CONTEST) {
            CampaignDetails details = new CampaignDetails(
                    HtmlSanitizer.sanitize(request.rules()),
                    HtmlSanitizer.sanitize(request.conditions()),
                    HtmlSanitizer.sanitize(request.awards())
            );
            campaign.setDetails(details);
        }

        return toDto(campaign);
    }

    @Transactional
    public CampaignDto updateStatus(UUID id, CampaignStatusUpdateRequestDto request) {
        Campaign campaign = findCampaignById(id);
        campaign.setStatus(request.status());
        if (request.status() == CampaignStatus.ACTIVE && campaign.getStartDatetime() == null) {
            campaign.setStartDatetime(OffsetDateTime.now());
        }
        campaign.setUpdatedAt(OffsetDateTime.now());
        return toDto(campaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignDto> listAll() {
        return campaignRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CampaignDto getById(UUID id) {
        return toDto(findCampaignById(id));
    }

    @Transactional(readOnly = true)
    public CampaignDto getBySlug(String slug) {
        Campaign campaign = campaignRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + slug));
        return toDto(campaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignDto> getActive() {
        return campaignRepository.findByStatusOrderByCreatedAtDesc(CampaignStatus.ACTIVE)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public CampaignDto linkBusiness(UUID campaignId, UUID businessId) {
        Campaign campaign = findCampaignById(campaignId);
        if (!businessProfileRepository.existsById(businessId)) {
            throw new IllegalArgumentException("Business not found: " + businessId);
        }
        if (campaignBusinessRepository.existsByCampaignIdAndBusinessId(campaignId, businessId)) {
            return toDto(campaign);
        }
        CampaignBusiness link = new CampaignBusiness();
        link.setCampaignId(campaignId);
        link.setBusinessId(businessId);
        link.setDisplayOrder(campaignBusinessRepository.findByCampaignIdOrderByDisplayOrderAsc(campaignId).size());
        campaignBusinessRepository.save(link);
        return toDto(campaign);
    }

    @Transactional
    public CampaignDto unlinkBusiness(UUID campaignId, UUID businessId) {
        Campaign campaign = findCampaignById(campaignId);
        campaignBusinessRepository.findByCampaignIdAndBusinessId(campaignId, businessId)
                .ifPresent(campaignBusinessRepository::delete);
        return toDto(campaign);
    }

    private Campaign findCampaignById(UUID id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));
    }

    private String uploadImage(UUID campaignId, MultipartFile image) {
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Campaign image must be 3 MB or smaller.");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are accepted.");
        }
        String ext = switch (contentType) {
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default           -> "jpg";
        };
        String subPath = "campaigns/" + campaignId + "/cover." + ext;
        try {
            storageService.saveFile(subPath, image.getInputStream(), contentType);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload campaign image.", e);
        }
        return subPath;
    }

    private String generateUniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s-]+", "-");
        if (!campaignRepository.existsBySlug(base)) {
            return base;
        }
        int suffix = 2;
        while (campaignRepository.existsBySlug(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }

    public CampaignDto toDto(Campaign campaign) {
        PhotoContestDetailsDto detailsDto = null;
        CampaignDetails details = campaign.getDetails();
        if (details != null) {
            detailsDto = new PhotoContestDetailsDto(
                    null,
                    details.rules(),
                    details.conditions(),
                    details.awards()
            );
        }
        String imageUrl = campaign.getImageSubpath() != null
                ? storageService.toPublicUrl(campaign.getImageSubpath())
                : null;

        List<CampaignBusinessDto> businesses = campaignBusinessRepository
                .findByCampaignIdOrderByDisplayOrderAsc(campaign.getId())
                .stream()
                .flatMap(cb -> businessProfileRepository.findById(cb.getBusinessId()).stream())
                .map(bp -> new CampaignBusinessDto(
                        bp.getId(),
                        bp.getName(),
                        bp.getSlug(),
                        bp.getThumbnailUrl() != null
                                ? storageService.toPublicUrl(bp.getThumbnailUrl())
                                : null
                ))
                .collect(Collectors.toList());

        return new CampaignDto(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getCampaignType(),
                campaign.getStatus(),
                campaign.getSlug(),
                imageUrl,
                campaign.getStartDatetime(),
                campaign.getEndDatetime(),
                detailsDto,
                businesses
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
