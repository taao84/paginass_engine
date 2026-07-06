package com.paginass.service;

import com.paginass.dto.PhotoContestEntryCreateRequestDto;
import com.paginass.dto.PhotoContestEntryDto;
import com.paginass.dto.PhotoContestLikeDto;
import com.paginass.entity.Campaign;
import com.paginass.entity.CampaignStatus;
import com.paginass.entity.PhotoContestEntry;
import com.paginass.entity.PhotoContestLike;
import com.paginass.entity.User;
import com.paginass.repository.CampaignRepository;
import com.paginass.repository.PhotoContestEntryRepository;
import com.paginass.repository.PhotoContestLikeRepository;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoContestService {

    private static final long MAX_FILE_SIZE = 4 * 1024 * 1024;

    private final PhotoContestEntryRepository entryRepository;
    private final PhotoContestLikeRepository likeRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final AppTimeService appTimeService;
    private final MailService mailService;

    @Autowired @Lazy
    private PhotoContestService self;

    @Transactional
    public PhotoContestEntryDto submitEntry(String campaignSlug,
                                            PhotoContestEntryCreateRequestDto request,
                                            MultipartFile photo) {
        User user = resolveCurrentUser();
        Campaign campaign = findActiveCampaign(campaignSlug);

        if (entryRepository.existsByCampaignIdAndUserId(campaign.getId(), user.getId())) {
            throw new IllegalStateException("Ya enviaste una foto a este concurso.");
        }

        validatePhoto(photo);

        String ext = resolveExtension(photo.getContentType());
        String subPath = "campaigns/" + campaign.getId() + "/" + UUID.randomUUID() + "." + ext;
        try {
            storageService.saveFile(subPath, photo.getInputStream(), photo.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo subir la foto. Intenta de nuevo.", e);
        }

        PhotoContestEntry entry = new PhotoContestEntry();
        entry.setCampaignId(campaign.getId());
        entry.setUserId(user.getId());
        entry.setTitle(request.title());
        entry.setDescription(request.description());
        entry.setPhotoSubpath(subPath);
        entryRepository.save(entry);

        return toDto(entry, user, 0L, false);
    }

    @Transactional
    public PhotoContestLikeDto toggleLike(UUID entryId) {
        User user = resolveCurrentUser();
        PhotoContestEntry entry = findEntryById(entryId);
        Campaign campaign = campaignRepository.findById(entry.getCampaignId())
                .orElseThrow(() -> new IllegalStateException("Campaign not found: " + entry.getCampaignId()));
        closeIfExpired(campaign);
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new IllegalStateException("La votación para este concurso ha finalizado.");
        }

        if (entry.getUserId().equals(user.getId())) {
            throw new IllegalStateException("No puedes votar por tu propia foto.");
        }

        Optional<PhotoContestLike> existing = likeRepository.findByEntryIdAndUserId(entryId, user.getId());
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
        } else {
            PhotoContestLike like = new PhotoContestLike();
            like.setEntryId(entryId);
            like.setUserId(user.getId());
            likeRepository.save(like);
        }

        long newCount = likeRepository.countByEntryId(entryId);
        boolean hasLiked = existing.isEmpty();
        return new PhotoContestLikeDto(entryId, newCount, hasLiked);
    }

    @Transactional(readOnly = true)
    public List<PhotoContestEntryDto> getGallery(String campaignSlug) {
        Campaign campaign = campaignRepository.findBySlug(campaignSlug)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignSlug));

        Optional<User> currentUser = resolveCurrentUserOptional();

        List<PhotoContestEntry> entries = isCurrentUserAdmin()
                ? entryRepository.findAllByCampaignIdRandomOrder(campaign.getId())
                : entryRepository.findEnabledByCampaignIdRandomOrder(campaign.getId());

        return entries.stream()
                .map(entry -> {
                    User author = userRepository.findById(entry.getUserId())
                            .orElseThrow(() -> new IllegalStateException("User not found: " + entry.getUserId()));
                    long likeCount = likeRepository.countByEntryId(entry.getId());
                    boolean hasLiked = currentUser
                            .map(u -> likeRepository.existsByEntryIdAndUserId(entry.getId(), u.getId()))
                            .orElse(false);
                    return toDto(entry, author, likeCount, hasLiked);
                }).toList();
    }

    @Transactional(readOnly = true)
    public PhotoContestEntryDto getEntry(UUID entryId) {
        PhotoContestEntry entry = findEntryById(entryId);
        if (entry.isDisabled() && !isCurrentUserAdmin()) {
            throw new IllegalArgumentException("Entry not found: " + entryId);
        }
        User author = userRepository.findById(entry.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + entry.getUserId()));
        long likeCount = likeRepository.countByEntryId(entryId);
        boolean hasLiked = resolveCurrentUserOptional()
                .map(u -> likeRepository.existsByEntryIdAndUserId(entryId, u.getId()))
                .orElse(false);
        return toDto(entry, author, likeCount, hasLiked);
    }

    @Transactional
    public void restoreEntry(UUID campaignId, UUID entryId) {
        PhotoContestEntry entry = findEntryById(entryId);
        if (!entry.getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Entry does not belong to campaign: " + campaignId);
        }
        entry.setDisabled(false);
    }

    @Transactional
    public void disableEntry(UUID campaignId, UUID entryId) {
        PhotoContestEntry entry = findEntryById(entryId);
        if (!entry.getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Entry does not belong to campaign: " + campaignId);
        }
        entry.setDisabled(true);
    }

    @Transactional
    public void deleteEntry(UUID campaignId, UUID entryId) {
        PhotoContestEntry entry = findEntryById(entryId);
        if (!entry.getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Entry does not belong to campaign: " + campaignId);
        }
        likeRepository.deleteAllByEntryId(entryId);
        try { storageService.deleteFile(entry.getPhotoSubpath()); } catch (IOException ignored) {}
        entryRepository.delete(entry);
    }

    @Transactional
    public List<PhotoContestEntryDto> getResults(String campaignSlug) {
        Campaign campaign = campaignRepository.findBySlug(campaignSlug)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignSlug));
        closeIfExpired(campaign);
        if (!isEndedOrClosed(campaign.getStatus())) {
            throw new IllegalStateException("Los resultados de este concurso aún no están disponibles.");
        }

        List<PhotoContestEntry> winners = entryRepository.findByCampaignIdAndWinnerRankIsNotNullOrderByWinnerRankAsc(campaign.getId());
        return winners.stream()
                .map(entry -> {
                    User author = userRepository.findById(entry.getUserId())
                            .orElseThrow(() -> new IllegalStateException("User not found: " + entry.getUserId()));
                    long likeCount = likeRepository.countByEntryId(entry.getId());
                    return toDto(entry, author, likeCount, false);
                }).toList();
    }

    @Transactional
    public PhotoContestEntryDto setWinnerRank(UUID campaignId, UUID entryId, Integer rank) {
        PhotoContestEntry entry = findEntryById(entryId);
        if (!entry.getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Entry does not belong to campaign: " + campaignId);
        }

        if (rank != null) {
            boolean rankTaken = entryRepository.findByCampaignIdAndWinnerRankIsNotNullOrderByWinnerRankAsc(campaignId).stream()
                    .anyMatch(e -> rank.equals(e.getWinnerRank()) && !e.getId().equals(entryId));
            if (rankTaken) {
                throw new IllegalStateException("El puesto " + rank + " ya fue asignado a otra foto.");
            }
        }

        entry.setWinnerRank(rank);
        User author = userRepository.findById(entry.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + entry.getUserId()));
        long likeCount = likeRepository.countByEntryId(entry.getId());
        return toDto(entry, author, likeCount, false);
    }

    public int notifyWinners(UUID campaignId, String subject, String message, List<UUID> entryIds) {
        List<String> recipientEmails = self.prepareWinnerNotifications(campaignId, entryIds);
        for (String email : recipientEmails) {
            mailService.sendCampaignMessage(email, subject, message);
        }
        return recipientEmails.size();
    }

    @Transactional
    public List<String> prepareWinnerNotifications(UUID campaignId, List<UUID> entryIds) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));
        if (!isEndedOrClosed(campaign.getStatus())) {
            throw new IllegalStateException("Solo se puede notificar a los ganadores una vez finalizado el concurso.");
        }

        OffsetDateTime now = appTimeService.now();
        List<String> emails = new ArrayList<>();
        for (UUID entryId : entryIds) {
            PhotoContestEntry entry = findEntryById(entryId);
            if (!entry.getCampaignId().equals(campaignId)) {
                throw new IllegalArgumentException("Entry does not belong to campaign: " + campaignId);
            }
            if (entry.getWinnerRank() == null) {
                throw new IllegalStateException("Solo se puede notificar a fotos marcadas como ganadoras.");
            }
            User author = userRepository.findById(entry.getUserId())
                    .orElseThrow(() -> new IllegalStateException("User not found: " + entry.getUserId()));
            entry.setNotifiedAt(now);
            emails.add(author.getEmail());
        }
        return emails;
    }

    private boolean isEndedOrClosed(CampaignStatus status) {
        return status == CampaignStatus.ENDED || status == CampaignStatus.CLOSED;
    }

    private Campaign findActiveCampaign(String slug) {
        Campaign campaign = campaignRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + slug));
        closeIfExpired(campaign);
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new IllegalStateException("Este concurso no está aceptando participaciones en este momento.");
        }
        return campaign;
    }

    private void closeIfExpired(Campaign campaign) {
        if (campaign.getStatus() == CampaignStatus.ACTIVE && !appTimeService.now().isBefore(campaign.getEndDatetime())) {
            campaign.setStatus(CampaignStatus.ENDED);
            campaign.setUpdatedAt(appTimeService.now());
            campaignRepository.save(campaign);
        }
    }

    private PhotoContestEntry findEntryById(UUID entryId) {
        return entryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + entryId));
    }

    private void validatePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar una foto.");
        }
        if (photo.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("La foto debe pesar 4 MB o menos.");
        }
        String contentType = photo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Solo se aceptan archivos de imagen.");
        }
    }

    private String resolveExtension(String contentType) {
        if (contentType == null) return "jpg";
        return switch (contentType) {
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            case "image/gif"  -> "gif";
            default           -> "jpg";
        };
    }

    private PhotoContestEntryDto toDto(PhotoContestEntry entry, User author, long likeCount, boolean hasLiked) {
        String photoUrl = storageService.toPublicUrl(entry.getPhotoSubpath());
        String authorName = author.getFullName() != null ? author.getFullName() : author.getEmail();
        return new PhotoContestEntryDto(
                entry.getId(),
                entry.getCampaignId(),
                entry.getTitle(),
                entry.getDescription(),
                photoUrl,
                authorName,
                author.getId(),
                likeCount,
                hasLiked,
                entry.isDisabled(),
                entry.getWinnerRank(),
                entry.getNotifiedAt() != null,
                entry.getCreatedAt()
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

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Optional<User> resolveCurrentUserOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        String email = auth.getPrincipal() instanceof UserDetails ud
                ? ud.getUsername()
                : auth.getName();
        return userRepository.findByEmail(email);
    }

}
