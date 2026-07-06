package com.paginass.service;

import com.paginass.dto.CreateHighlightRequestDto;
import com.paginass.dto.HighlightableAdDto;
import com.paginass.dto.HighlightableBusinessDto;
import com.paginass.dto.HighlightableCampaignDto;
import com.paginass.dto.HighlightCardDto;
import com.paginass.dto.HighlightSummaryDto;
import com.paginass.entity.AdCategory;
import com.paginass.entity.Advertisement;
import com.paginass.entity.BusinessProfile;
import com.paginass.entity.BusinessType;
import com.paginass.entity.Campaign;
import com.paginass.entity.CampaignStatus;
import com.paginass.entity.Highlight;
import com.paginass.entity.HighlightType;
import com.paginass.entity.User;
import com.paginass.repository.AdvertisementRepository;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.CampaignRepository;
import com.paginass.repository.HighlightRepository;
import com.paginass.repository.PhotoContestEntryRepository;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HighlightService {

    private static final String CAMPAIGN_BADGE = "Campaña";

    private final HighlightRepository highlightRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final AdvertisementRepository advertisementRepository;
    private final CampaignRepository campaignRepository;
    private final PhotoContestEntryRepository photoContestEntryRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Transactional
    public HighlightSummaryDto createHighlight(CreateHighlightRequestDto request) {
        validateReference(request.type(), request.referenceId());

        User admin = resolveCurrentUser();
        Highlight highlight = new Highlight();
        highlight.setType(request.type());
        highlight.setReferenceId(request.referenceId());
        highlight.setExpiresAt(Instant.now().plus(request.lifetime().getDuration()));
        highlight.setCreatedBy(admin.getId());

        Highlight saved = highlightRepository.save(highlight);
        return toSummaryDto(saved, resolveTitle(saved), resolveBadge(saved));
    }

    @Transactional(readOnly = true)
    public List<HighlightCardDto> getActiveHighlights() {
        return highlightRepository.findAllByEnabledTrueOrderByCreatedAtDesc().stream()
                .map(this::toCardDtoOrEmpty)
                .flatMap(Optional::stream)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HighlightSummaryDto> listAdminHighlights() {
        return highlightRepository.findAllByEnabledTrueOrderByCreatedAtDesc().stream()
                .map(h -> toSummaryDto(h, resolveTitle(h), resolveBadge(h)))
                .toList();
    }

    @Transactional
    public void disableHighlight(UUID id) {
        Highlight highlight = highlightRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Highlight not found: " + id));
        highlight.setEnabled(false);
    }

    @Transactional(readOnly = true)
    public List<HighlightableBusinessDto> searchBusinesses(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return businessProfileRepository.searchActiveByName("%" + query.trim() + "%").stream()
                .map(p -> new HighlightableBusinessDto(p.getId(), p.getName(), badge(BusinessType.valueOf(p.getBizType()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HighlightableAdDto> searchAds(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return advertisementRepository.searchActiveByTitle("%" + query.trim() + "%").stream()
                .map(p -> new HighlightableAdDto(p.getId(), p.getTitle(), badge(AdCategory.valueOf(p.getCategory()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HighlightableCampaignDto> searchCampaigns(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return campaignRepository
                .findByStatusInAndNameContainingIgnoreCaseOrderByNameAsc(
                        List.of(CampaignStatus.ACTIVE, CampaignStatus.ENDED), query.trim())
                .stream()
                .map(c -> new HighlightableCampaignDto(c.getId(), c.getName(), CAMPAIGN_BADGE))
                .toList();
    }

    @Transactional
    public int expireHighlights() {
        List<Highlight> expired = highlightRepository.findAllByEnabledTrueAndExpiresAtBefore(Instant.now());
        expired.forEach(h -> h.setEnabled(false));
        return expired.size();
    }

    private void validateReference(HighlightType type, UUID referenceId) {
        switch (type) {
            case BUSINESS -> businessProfileRepository.findById(referenceId)
                    .orElseThrow(() -> new IllegalArgumentException("Business not found: " + referenceId));
            case ADVERTISEMENT -> advertisementRepository.findById(referenceId)
                    .orElseThrow(() -> new IllegalArgumentException("Advertisement not found: " + referenceId));
            case CAMPAIGN -> campaignRepository.findById(referenceId)
                    .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + referenceId));
        }
    }

    private Optional<HighlightCardDto> toCardDtoOrEmpty(Highlight highlight) {
        return switch (highlight.getType()) {
            case BUSINESS -> businessProfileRepository.findById(highlight.getReferenceId())
                    .filter(BusinessProfile::isActive)
                    .map(b -> buildBusinessCard(highlight, b));
            case ADVERTISEMENT -> advertisementRepository.findById(highlight.getReferenceId())
                    .filter(Advertisement::isActive)
                    .filter(a -> a.getExpiresAt() == null || a.getExpiresAt().isAfter(Instant.now()))
                    .map(a -> buildAdCard(highlight, a));
            case CAMPAIGN -> campaignRepository.findById(highlight.getReferenceId())
                    .filter(this::isCampaignHighlightable)
                    .map(c -> buildCampaignCard(highlight, c));
        };
    }

    private boolean isCampaignHighlightable(Campaign campaign) {
        if (campaign.getStatus() == CampaignStatus.ACTIVE) {
            return true;
        }
        return campaign.getStatus() == CampaignStatus.ENDED
                && photoContestEntryRepository.existsByCampaignIdAndWinnerRankIsNotNull(campaign.getId());
    }

    private HighlightCardDto buildBusinessCard(Highlight highlight, BusinessProfile business) {
        String thumbnailUrl = business.getBannerUrl() != null
                ? storageService.toPublicUrl(business.getBannerUrl())
                : null;
        String badge = badge(business.getBizType());
        String description = business.getDescription() != null
                ? business.getDescription()
                : "";
        return new HighlightCardDto(
                highlight.getId(),
                HighlightType.BUSINESS,
                business.getName(),
                thumbnailUrl,
                "/" + business.getSlug(),
                badge,
                description
        );
    }

    private HighlightCardDto buildAdCard(Highlight highlight, Advertisement ad) {
        String thumbnailUrl = ad.getImageUrls().isEmpty()
                ? null
                : storageService.toPublicUrl(ad.getImageUrls().get(0));
        String badge = badge(ad.getCategory());
        return new HighlightCardDto(
                highlight.getId(),
                HighlightType.ADVERTISEMENT,
                ad.getTitle(),
                thumbnailUrl,
                "/ads/" + ad.getId(),
                badge,
                ad.getContent()
        );
    }

    private HighlightCardDto buildCampaignCard(Highlight highlight, Campaign campaign) {
        String thumbnailUrl = campaign.getImageSubpath() != null
                ? storageService.toPublicUrl(campaign.getImageSubpath())
                : null;
        String linkPath = campaign.getStatus() == CampaignStatus.ENDED
                ? "/concursos/" + campaign.getSlug() + "/resultados"
                : "/concursos/" + campaign.getSlug();
        return new HighlightCardDto(
                highlight.getId(),
                HighlightType.CAMPAIGN,
                campaign.getName(),
                thumbnailUrl,
                linkPath,
                CAMPAIGN_BADGE,
                campaign.getDescription() != null ? campaign.getDescription() : ""
        );
    }

    private String resolveTitle(Highlight highlight) {
        return switch (highlight.getType()) {
            case BUSINESS -> businessProfileRepository.findById(highlight.getReferenceId())
                    .map(BusinessProfile::getName).orElse("(negocio eliminado)");
            case ADVERTISEMENT -> advertisementRepository.findById(highlight.getReferenceId())
                    .map(Advertisement::getTitle).orElse("(anuncio eliminado)");
            case CAMPAIGN -> campaignRepository.findById(highlight.getReferenceId())
                    .map(Campaign::getName).orElse("(concurso eliminado)");
        };
    }

    private String resolveBadge(Highlight highlight) {
        return switch (highlight.getType()) {
            case BUSINESS -> businessProfileRepository.findById(highlight.getReferenceId())
                    .map(b -> badge(b.getBizType())).orElse("");
            case ADVERTISEMENT -> advertisementRepository.findById(highlight.getReferenceId())
                    .map(a -> badge(a.getCategory())).orElse("");
            case CAMPAIGN -> CAMPAIGN_BADGE;
        };
    }

    private HighlightSummaryDto toSummaryDto(Highlight highlight, String title, String badge) {
        return new HighlightSummaryDto(
                highlight.getId(),
                highlight.getType(),
                title,
                badge,
                highlight.getExpiresAt(),
                highlight.isEnabled()
        );
    }

    private String badge(BusinessType type) {
        return switch (type) {
            case BRICK_AND_MORTAR -> "Negocio";
            case INFORMAL         -> "Informal";
            case GIG              -> "Servicio";
        };
    }

    private String badge(AdCategory category) {
        return switch (category) {
            case SALE  -> "Venta";
            case JOB   -> "Empleo";
            case EVENT -> "Evento";
        };
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
