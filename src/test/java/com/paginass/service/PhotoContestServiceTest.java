package com.paginass.service;

import com.paginass.dto.PhotoContestEntryCreateRequestDto;
import com.paginass.dto.PhotoContestEntryDto;
import com.paginass.dto.PhotoContestLikeDto;
import com.paginass.entity.Campaign;
import com.paginass.entity.CampaignStatus;
import com.paginass.entity.PhotoContestEntry;
import com.paginass.entity.PhotoContestLike;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.CampaignRepository;
import com.paginass.repository.PhotoContestEntryRepository;
import com.paginass.repository.PhotoContestLikeRepository;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhotoContestServiceTest {

    @Mock private PhotoContestEntryRepository entryRepository;
    @Mock private PhotoContestLikeRepository likeRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private PhotoContestService photoContestService;

    private User contributor;
    private Campaign activeCampaign;

    @BeforeEach
    void setup() {
        contributor = new User();
        contributor.setId(UUID.randomUUID());
        contributor.setEmail("user@example.com");
        contributor.setFullName("Test User");
        contributor.setRole(UserRole.CONTRIBUTOR);

        activeCampaign = new Campaign();
        activeCampaign.setId(UUID.randomUUID());
        activeCampaign.setSlug("foto-2025");
        activeCampaign.setStatus(CampaignStatus.ACTIVE);
        activeCampaign.setStartDatetime(OffsetDateTime.now().minusDays(1));
        activeCampaign.setEndDatetime(OffsetDateTime.now().plusDays(29));

        UserDetails principal = new org.springframework.security.core.userdetails.User(
                "user@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_CONTRIBUTOR")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(contributor));
        when(campaignRepository.findBySlug("foto-2025")).thenReturn(Optional.of(activeCampaign));
        when(campaignRepository.findById(activeCampaign.getId())).thenAnswer(inv -> Optional.of(activeCampaign));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitEntry_savesEntryAndUploadsPhoto() throws IOException {
        when(entryRepository.existsByCampaignIdAndUserId(activeCampaign.getId(), contributor.getId()))
                .thenReturn(false);
        when(storageService.saveFile(anyString(), any(), anyString())).thenReturn("ok");
        when(storageService.toPublicUrl(anyString())).thenReturn("https://cdn.example.com/img.jpg");
        when(entryRepository.save(any())).thenAnswer(inv -> {
            PhotoContestEntry e = inv.getArgument(0);
            e.setCampaignId(activeCampaign.getId());
            e.setUserId(contributor.getId());
            return e;
        });
        when(userRepository.findById(contributor.getId())).thenReturn(Optional.of(contributor));

        MockMultipartFile photo = new MockMultipartFile("photo", "cat.jpg", "image/jpeg", new byte[100]);

        PhotoContestEntryDto result = photoContestService.submitEntry(
                "foto-2025",
                new PhotoContestEntryCreateRequestDto("My Cat", "A great shot"),
                photo);

        assertThat(result.title()).isEqualTo("My Cat");
        assertThat(result.authorName()).isEqualTo("Test User");
        assertThat(result.likeCount()).isEqualTo(0L);
        assertThat(result.hasLiked()).isFalse();
        verify(storageService).saveFile(anyString(), any(), any());
    }

    @Test
    void submitEntry_rejectsWhenUserAlreadyHasEntry() {
        when(entryRepository.existsByCampaignIdAndUserId(activeCampaign.getId(), contributor.getId()))
                .thenReturn(true);

        MockMultipartFile photo = new MockMultipartFile("photo", "img.jpg", "image/jpeg", new byte[10]);

        assertThatThrownBy(() -> photoContestService.submitEntry(
                "foto-2025",
                new PhotoContestEntryCreateRequestDto("Title", null),
                photo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya enviaste");

        verify(entryRepository, never()).save(any());
    }

    @Test
    void submitEntry_rejectsWhenCampaignNotActive() {
        activeCampaign.setStatus(CampaignStatus.ENDED);

        MockMultipartFile photo = new MockMultipartFile("photo", "img.jpg", "image/jpeg", new byte[10]);

        assertThatThrownBy(() -> photoContestService.submitEntry(
                "foto-2025",
                new PhotoContestEntryCreateRequestDto("Title", null),
                photo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está aceptando");
    }

    @Test
    void submitEntry_rejectsOversizedFile() {
        when(entryRepository.existsByCampaignIdAndUserId(any(), any())).thenReturn(false);

        byte[] bigFile = new byte[5 * 1024 * 1024];
        MockMultipartFile photo = new MockMultipartFile("photo", "big.jpg", "image/jpeg", bigFile);

        assertThatThrownBy(() -> photoContestService.submitEntry(
                "foto-2025",
                new PhotoContestEntryCreateRequestDto("Title", null),
                photo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4 MB o menos");
    }

    @Test
    void submitEntry_rejectsNonImageFile() {
        when(entryRepository.existsByCampaignIdAndUserId(any(), any())).thenReturn(false);

        MockMultipartFile pdf = new MockMultipartFile("photo", "doc.pdf", "application/pdf", new byte[100]);

        assertThatThrownBy(() -> photoContestService.submitEntry(
                "foto-2025",
                new PhotoContestEntryCreateRequestDto("Title", null),
                pdf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo se aceptan");
    }

    @Test
    void toggleLike_addsLikeWhenNotPresent() {
        PhotoContestEntry entry = buildEntry(UUID.randomUUID());
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(likeRepository.findByEntryIdAndUserId(entry.getId(), contributor.getId()))
                .thenReturn(Optional.empty());
        when(likeRepository.countByEntryId(entry.getId())).thenReturn(1L);

        PhotoContestLikeDto result = photoContestService.toggleLike(entry.getId());

        assertThat(result.hasLiked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(1L);
        verify(likeRepository).save(any(PhotoContestLike.class));
    }

    @Test
    void toggleLike_removesLikeWhenPresent() {
        PhotoContestEntry entry = buildEntry(UUID.randomUUID());
        PhotoContestLike existing = new PhotoContestLike();
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(likeRepository.findByEntryIdAndUserId(entry.getId(), contributor.getId()))
                .thenReturn(Optional.of(existing));
        when(likeRepository.countByEntryId(entry.getId())).thenReturn(0L);

        PhotoContestLikeDto result = photoContestService.toggleLike(entry.getId());

        assertThat(result.hasLiked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(0L);
        verify(likeRepository).delete(existing);
    }

    @Test
    void toggleLike_rejectsOwnEntry() {
        PhotoContestEntry entry = buildEntry(contributor.getId());
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> photoContestService.toggleLike(entry.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No puedes votar");
    }

    @Test
    void getGallery_anyoneCanSeeDraftCampaignViaDirectLink() {
        activeCampaign.setStatus(CampaignStatus.DRAFT);
        when(entryRepository.findEnabledByCampaignIdRandomOrder(activeCampaign.getId())).thenReturn(List.of());

        List<PhotoContestEntryDto> result = photoContestService.getGallery("foto-2025");

        assertThat(result).isEmpty();
    }

    @Test
    void getGallery_adminReceivesAllEntriesIncludingDisabled() {
        setAuthAs("admin@example.com", "ROLE_ADMIN");
        UUID authorId = UUID.randomUUID();
        PhotoContestEntry disabled = buildEntry(authorId);
        disabled.setDisabled(true);
        User author = new User();
        author.setId(authorId);
        author.setEmail("author@example.com");
        when(entryRepository.findAllByCampaignIdRandomOrder(activeCampaign.getId())).thenReturn(List.of(disabled));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(storageService.toPublicUrl(anyString())).thenReturn("https://cdn.example.com/img.jpg");
        when(likeRepository.countByEntryId(disabled.getId())).thenReturn(0L);

        List<PhotoContestEntryDto> result = photoContestService.getGallery("foto-2025");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).disabled()).isTrue();
    }

    @Test
    void getGallery_nonAdminReceivesOnlyEnabledEntries() {
        when(entryRepository.findEnabledByCampaignIdRandomOrder(activeCampaign.getId())).thenReturn(List.of());

        photoContestService.getGallery("foto-2025");

        verify(entryRepository).findEnabledByCampaignIdRandomOrder(activeCampaign.getId());
        verify(entryRepository, never()).findAllByCampaignIdRandomOrder(any());
    }

    @Test
    void restoreEntry_clearsDisabledFlag() {
        PhotoContestEntry entry = buildEntry(UUID.randomUUID());
        entry.setDisabled(true);
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        photoContestService.restoreEntry(activeCampaign.getId(), entry.getId());

        assertThat(entry.isDisabled()).isFalse();
    }

    @Test
    void restoreEntry_rejectsEntryFromOtherCampaign() {
        PhotoContestEntry entry = buildEntry(UUID.randomUUID());
        entry.setCampaignId(UUID.randomUUID());
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> photoContestService.restoreEntry(activeCampaign.getId(), entry.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void getEntry_anyoneCanSeeDraftEntryViaDirectLink() {
        activeCampaign.setStatus(CampaignStatus.DRAFT);
        UUID authorId = UUID.randomUUID();
        PhotoContestEntry entry = buildEntry(authorId);
        User author = new User();
        author.setId(authorId);
        author.setEmail("author@example.com");
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(storageService.toPublicUrl(anyString())).thenReturn("https://cdn.example.com/img.jpg");
        when(likeRepository.countByEntryId(entry.getId())).thenReturn(0L);

        PhotoContestEntryDto result = photoContestService.getEntry(entry.getId());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(entry.getId());
    }

    @Test
    void disableEntry_setsDisabledFlag() {
        PhotoContestEntry entry = buildEntry(UUID.randomUUID());
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        photoContestService.disableEntry(activeCampaign.getId(), entry.getId());

        assertThat(entry.isDisabled()).isTrue();
    }

    @Test
    void disableEntry_rejectsEntryFromOtherCampaign() {
        PhotoContestEntry entry = buildEntry(UUID.randomUUID());
        entry.setCampaignId(UUID.randomUUID());
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> photoContestService.disableEntry(activeCampaign.getId(), entry.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void deleteEntry_removesLikesAndDeletesEntry() throws Exception {
        PhotoContestEntry entry = buildEntry(UUID.randomUUID());
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        photoContestService.deleteEntry(activeCampaign.getId(), entry.getId());

        verify(likeRepository).deleteAllByEntryId(entry.getId());
        verify(storageService).deleteFile(entry.getPhotoSubpath());
        verify(entryRepository).delete(entry);
    }

    @Test
    void deleteEntry_rejectsEntryFromOtherCampaign() {
        PhotoContestEntry entry = buildEntry(UUID.randomUUID());
        entry.setCampaignId(UUID.randomUUID());
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> photoContestService.deleteEntry(activeCampaign.getId(), entry.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void getEntry_disabledEntryHiddenFromNonAdmin() {
        PhotoContestEntry entry = buildEntry(UUID.randomUUID());
        entry.setDisabled(true);
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> photoContestService.getEntry(entry.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Entry not found");
    }

    @Test
    void getEntry_disabledEntryVisibleToAdmin() {
        setAuthAs("admin@example.com", "ROLE_ADMIN");
        UUID authorId = UUID.randomUUID();
        PhotoContestEntry entry = buildEntry(authorId);
        entry.setDisabled(true);
        User author = new User();
        author.setId(authorId);
        author.setEmail("author@example.com");
        when(entryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(storageService.toPublicUrl(anyString())).thenReturn("https://cdn.example.com/img.jpg");
        when(likeRepository.countByEntryId(entry.getId())).thenReturn(0L);

        PhotoContestEntryDto result = photoContestService.getEntry(entry.getId());

        assertThat(result).isNotNull();
    }

    private void setAuthAs(String email, String role) {
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                email, "", List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private PhotoContestEntry buildEntry(UUID ownerId) {
        PhotoContestEntry entry = new PhotoContestEntry();
        entry.setId(UUID.randomUUID());
        entry.setCampaignId(activeCampaign.getId());
        entry.setUserId(ownerId);
        entry.setTitle("Entry");
        entry.setPhotoSubpath("campaigns/x/y.jpg");
        return entry;
    }
}
