package com.paginass.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paginass.dto.DirectoryPageDto;
import com.paginass.dto.TagCloudItemDto;
import com.paginass.entity.BusinessProfile;
import com.paginass.entity.BusinessProfileEdit;
import com.paginass.entity.BusinessType;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.BusinessClaimRepository;
import com.paginass.repository.BusinessProfileEditRepository;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessProfileServiceTest {

    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private BusinessClaimRepository businessClaimRepository;
    @Mock private BusinessProfileEditRepository businessProfileEditRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private BusinessProfileService service;

    @BeforeEach
    void setupSecurityContext() {
        var auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // getEditHistory
    // -------------------------------------------------------------------------

    @Test
    void getEditHistory_returnsEditsNewestFirst() {
        UUID businessId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();

        BusinessProfile profile = unclaimedProfile();
        profile.setId(businessId);

        User editor = userWithRole(UserRole.EDITOR);
        editor.setId(editorId);
        editor.setFullName("Test Editor");

        BusinessProfileEdit edit1 = editRecord(businessId, editorId, OffsetDateTime.now(ZoneOffset.UTC).minusHours(2));
        BusinessProfileEdit edit2 = editRecord(businessId, editorId, OffsetDateTime.now(ZoneOffset.UTC));

        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(profile));
        when(businessProfileEditRepository.findByBusinessProfileIdOrderByEditedAtDesc(businessId))
                .thenReturn(List.of(edit2, edit1));
        when(userRepository.findAllById(any())).thenReturn(List.of(editor));

        var result = service.getEditHistory(businessId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).editedByName()).isEqualTo("Test Editor");
        assertThat(result.get(0).editedAt()).isAfter(result.get(1).editedAt());
    }

    @Test
    void getEditHistory_returnsEmptyListWhenNoEdits() {
        UUID businessId = UUID.randomUUID();
        BusinessProfile profile = unclaimedProfile();
        profile.setId(businessId);

        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(profile));
        when(businessProfileEditRepository.findByBusinessProfileIdOrderByEditedAtDesc(businessId))
                .thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of());

        var result = service.getEditHistory(businessId);

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // restoreFromEdit
    // -------------------------------------------------------------------------

    @Test
    void restoreFromEdit_appliesSnapshotFieldsToProfile() {
        UUID businessId = UUID.randomUUID();
        UUID editId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        BusinessProfile profile = unclaimedProfile();
        profile.setId(businessId);
        profile.setName("Old Name");

        Map<String, Object> snapshot = Map.of(
                "name", "Restored Name",
                "description", "Restored desc",
                "tags", List.of("tag1", "tag2"),
                "deliversToHome", true,
                "contactInfo", Map.of("phones", "555-1234"),
                "imageUrls", List.of()
        );

        BusinessProfileEdit edit = new BusinessProfileEdit();
        edit.setId(editId);
        edit.setBusinessProfileId(businessId);
        edit.setEditedBy(adminId);
        edit.setSnapshot(snapshot);
        edit.setEditedAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1));

        User admin = userWithRole(UserRole.ADMIN);
        admin.setId(adminId);
        admin.setEmail("admin@example.com");

        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(profile));
        when(businessProfileEditRepository.findById(editId)).thenReturn(Optional.of(edit));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileEditRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        service.restoreFromEdit(businessId, editId);

        assertThat(profile.getName()).isEqualTo("Restored Name");
        assertThat(profile.getDescription()).isEqualTo("Restored desc");
        assertThat(profile.isDeliversToHome()).isTrue();
    }

    @Test
    void restoreFromEdit_savesNewHistoryEntryAfterRestore() {
        UUID businessId = UUID.randomUUID();
        UUID editId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        BusinessProfile profile = unclaimedProfile();
        profile.setId(businessId);

        BusinessProfileEdit edit = new BusinessProfileEdit();
        edit.setId(editId);
        edit.setBusinessProfileId(businessId);
        edit.setEditedBy(adminId);
        edit.setSnapshot(Map.of("name", "Restored", "deliversToHome", false));
        edit.setEditedAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1));

        User admin = userWithRole(UserRole.ADMIN);
        admin.setId(adminId);
        admin.setEmail("admin@example.com");

        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(profile));
        when(businessProfileEditRepository.findById(editId)).thenReturn(Optional.of(edit));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileEditRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        service.restoreFromEdit(businessId, editId);

        ArgumentCaptor<BusinessProfileEdit> captor = ArgumentCaptor.forClass(BusinessProfileEdit.class);
        verify(businessProfileEditRepository).save(captor.capture());
        BusinessProfileEdit savedEdit = captor.getValue();

        assertThat(savedEdit.getEditedBy()).isEqualTo(adminId);
        assertThat(savedEdit.getEditedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(savedEdit.getSnapshot()).containsKey("name");
    }

    @Test
    void restoreFromEdit_throwsWhenEditBelongsToDifferentBusiness() {
        UUID businessId = UUID.randomUUID();
        UUID editId = UUID.randomUUID();

        BusinessProfile profile = unclaimedProfile();
        profile.setId(businessId);

        BusinessProfileEdit edit = new BusinessProfileEdit();
        edit.setId(editId);
        edit.setBusinessProfileId(UUID.randomUUID()); // different business

        User admin = userWithRole(UserRole.ADMIN);
        admin.setEmail("admin@example.com");

        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(profile));
        when(businessProfileEditRepository.findById(editId)).thenReturn(Optional.of(edit));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.restoreFromEdit(businessId, editId))
                .isInstanceOf(ResponseStatusException.class);
    }

    // -------------------------------------------------------------------------
    // updateProfile — bizType
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_updatesBizTypeWhenProvided() {
        UUID profileId = UUID.randomUUID();

        BusinessProfile profile = unclaimedProfile();
        profile.setId(profileId);
        profile.setBizType(BusinessType.INFORMAL);

        User admin = userWithRole(UserRole.ADMIN);
        admin.setEmail("admin@example.com");

        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileEditRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        var request = new com.paginass.dto.BusinessProfileUpdateRequestDto(
                "Name", null, null, null, null, null, false, "BRICK_AND_MORTAR");

        service.updateProfile(profileId, request);

        assertThat(profile.getBizType()).isEqualTo(BusinessType.BRICK_AND_MORTAR);
    }

    @Test
    void updateProfile_throwsBadRequestForUnknownBizType() {
        UUID profileId = UUID.randomUUID();

        BusinessProfile profile = unclaimedProfile();
        profile.setId(profileId);

        User admin = userWithRole(UserRole.ADMIN);
        admin.setEmail("admin@example.com");

        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        var request = new com.paginass.dto.BusinessProfileUpdateRequestDto(
                "Name", null, null, null, null, null, false, "INVALID_TYPE");

        assertThatThrownBy(() -> service.updateProfile(profileId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid bizType");
    }

    @Test
    void updateProfile_leavesBizTypeUnchangedWhenNull() {
        UUID profileId = UUID.randomUUID();

        BusinessProfile profile = unclaimedProfile();
        profile.setId(profileId);
        profile.setBizType(BusinessType.GIG);

        User admin = userWithRole(UserRole.ADMIN);
        admin.setEmail("admin@example.com");

        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileEditRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        var request = new com.paginass.dto.BusinessProfileUpdateRequestDto(
                "Name", null, null, null, null, null, false, null);

        service.updateProfile(profileId, request);

        assertThat(profile.getBizType()).isEqualTo(BusinessType.GIG);
    }

    // -------------------------------------------------------------------------
    // search — prefix query transformation
    // -------------------------------------------------------------------------

    @Test
    void search_transformsSingleWordToPrefixQuery() {
        when(businessProfileRepository.searchBusinessProfiles("restaura", "restaura:*", 10, 0))
                .thenReturn(Collections.emptyList());
        when(businessProfileRepository.countSearchResults("restaura", "restaura:*")).thenReturn(0L);

        service.search("restaura", 0);

        verify(businessProfileRepository).searchBusinessProfiles("restaura", "restaura:*", 10, 0);
        verify(businessProfileRepository).countSearchResults("restaura", "restaura:*");
    }

    @Test
    void search_transformsMultiWordToPrefixQuery() {
        when(businessProfileRepository.searchBusinessProfiles("pollo asado", "pollo:* & asado:*", 10, 0))
                .thenReturn(Collections.emptyList());
        when(businessProfileRepository.countSearchResults("pollo asado", "pollo:* & asado:*")).thenReturn(0L);

        service.search("pollo asado", 0);

        verify(businessProfileRepository).searchBusinessProfiles("pollo asado", "pollo:* & asado:*", 10, 0);
    }

    @Test
    void search_stripsSpecialCharactersFromQuery() {
        when(businessProfileRepository.searchBusinessProfiles("restaurante!", "restaurante:*", 10, 0))
                .thenReturn(Collections.emptyList());
        when(businessProfileRepository.countSearchResults("restaurante!", "restaurante:*")).thenReturn(0L);

        service.search("restaurante!", 0);

        verify(businessProfileRepository).searchBusinessProfiles("restaurante!", "restaurante:*", 10, 0);
    }

    @Test
    void search_stripsTsqueryPhraseOperator() {
        when(businessProfileRepository.searchBusinessProfiles("pollo <-> asado", "pollo:* & asado:*", 10, 0))
                .thenReturn(Collections.emptyList());
        when(businessProfileRepository.countSearchResults("pollo <-> asado", "pollo:* & asado:*")).thenReturn(0L);

        service.search("pollo <-> asado", 0);

        verify(businessProfileRepository).searchBusinessProfiles("pollo <-> asado", "pollo:* & asado:*", 10, 0);
    }

    @Test
    void search_returnsEmptyResultsWhenQueryReducesToBlank() {
        var result = service.search("!&|()", 0);

        assertThat(result.results()).isEmpty();
        assertThat(result.totalResults()).isEqualTo(0);
        verify(businessProfileRepository, never()).searchBusinessProfiles(any(), any(), anyInt(), anyInt());
    }

    // -------------------------------------------------------------------------
    // getDirectoryByLetter
    // -------------------------------------------------------------------------

    @Test
    void getDirectoryByLetter_returnsEntriesForMatchingLetter() {
        Object[] row = new Object[]{"Artesanias Lopez", "artesanias-lopez", "BRICK_AND_MORTAR"};
        when(businessProfileRepository.findByFirstLetter("A", 50, 0))
                .thenReturn(Collections.singletonList(row));
        when(businessProfileRepository.countByFirstLetter("A")).thenReturn(1L);

        DirectoryPageDto result = service.getDirectoryByLetter("A", 0);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Artesanias Lopez");
        assertThat(result.content().get(0).bizType()).isEqualTo("BRICK_AND_MORTAR");
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    void getDirectoryByLetter_normalizesLowercaseLetter() {
        when(businessProfileRepository.findByFirstLetter("B", 50, 0))
                .thenReturn(Collections.emptyList());
        when(businessProfileRepository.countByFirstLetter("B")).thenReturn(0L);

        DirectoryPageDto result = service.getDirectoryByLetter("b", 0);

        verify(businessProfileRepository).findByFirstLetter("B", 50, 0);
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0L);
    }

    @Test
    void getDirectoryByLetter_hashBucketUsesNonAlphaQuery() {
        Object[] row = new Object[]{"3D Impresiones", "3d-impresiones", "GIG"};
        when(businessProfileRepository.findByNonAlphaStart(50, 0))
                .thenReturn(Collections.singletonList(row));
        when(businessProfileRepository.countByNonAlphaStart()).thenReturn(1L);

        DirectoryPageDto result = service.getDirectoryByLetter("#", 0);

        verify(businessProfileRepository).findByNonAlphaStart(50, 0);
        verify(businessProfileRepository, never()).findByFirstLetter(any(), anyInt(), anyInt());
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("3D Impresiones");
    }

    // -------------------------------------------------------------------------
    // getDirectoryByTag
    // -------------------------------------------------------------------------

    @Test
    void getDirectoryByTag_returnsEntriesForMatchingTag() {
        Object[] row = new Object[]{"Ceramica del Valle", "ceramica-del-valle", "INFORMAL"};
        when(businessProfileRepository.findByTag("ceramica", 50, 0))
                .thenReturn(Collections.singletonList(row));
        when(businessProfileRepository.countByTag("ceramica")).thenReturn(1L);

        DirectoryPageDto result = service.getDirectoryByTag("ceramica", 0);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Ceramica del Valle");
        assertThat(result.content().get(0).bizType()).isEqualTo("INFORMAL");
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    void getDirectoryByTag_returnsEmptyWhenNoBusinessHasTag() {
        when(businessProfileRepository.findByTag("nonexistent", 50, 0))
                .thenReturn(Collections.emptyList());
        when(businessProfileRepository.countByTag("nonexistent")).thenReturn(0L);

        DirectoryPageDto result = service.getDirectoryByTag("nonexistent", 0);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0L);
        assertThat(result.totalPages()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // getDirectoryTags
    // -------------------------------------------------------------------------

    @Test
    void getDirectoryTags_returnsTagsWithCounts() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"ceramica", 5L});
        rows.add(new Object[]{"artesanias", 3L});
        rows.add(new Object[]{"ropa", 1L});
        when(businessProfileRepository.findAllTagsWithCount()).thenReturn(rows);

        List<TagCloudItemDto> result = service.getDirectoryTags();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).tag()).isEqualTo("ceramica");
        assertThat(result.get(0).count()).isEqualTo(5L);
        assertThat(result.get(1).tag()).isEqualTo("artesanias");
        assertThat(result.get(2).tag()).isEqualTo("ropa");
    }

    @Test
    void getDirectoryTags_returnsEmptyListWhenNoTags() {
        when(businessProfileRepository.findAllTagsWithCount()).thenReturn(Collections.emptyList());

        List<TagCloudItemDto> result = service.getDirectoryTags();

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // updateProfile — tag normalization
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_normalizesTagsOnSave() {
        UUID profileId = UUID.randomUUID();

        BusinessProfile profile = unclaimedProfile();
        profile.setId(profileId);

        User admin = userWithRole(UserRole.ADMIN);
        admin.setEmail("admin@example.com");

        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileEditRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        var request = new com.paginass.dto.BusinessProfileUpdateRequestDto(
                "Name", null, null, null, null,
                List.of("Plomería", "Instalación Eléctrica", "PINTURA"),
                false, null);

        service.updateProfile(profileId, request);

        assertThat(profile.getTags())
                .containsExactlyInAnyOrder("plomeria", "instalacion electrica", "pintura");
    }

    @Test
    void updateProfile_deduplicatesTagsAfterNormalization() {
        UUID profileId = UUID.randomUUID();

        BusinessProfile profile = unclaimedProfile();
        profile.setId(profileId);

        User admin = userWithRole(UserRole.ADMIN);
        admin.setEmail("admin@example.com");

        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileEditRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        var request = new com.paginass.dto.BusinessProfileUpdateRequestDto(
                "Name", null, null, null, null,
                List.of("ceramica", "Cerámica", "CERAMICA"),
                false, null);

        service.updateProfile(profileId, request);

        assertThat(profile.getTags()).containsExactly("ceramica");
    }

    @Test
    void updateProfile_handlesNullAndEmptyTagList() {
        UUID profileId = UUID.randomUUID();

        BusinessProfile profile = unclaimedProfile();
        profile.setId(profileId);

        User admin = userWithRole(UserRole.ADMIN);
        admin.setEmail("admin@example.com");

        when(businessProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileEditRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        var requestNull = new com.paginass.dto.BusinessProfileUpdateRequestDto(
                "Name", null, null, null, null, null, false, null);
        service.updateProfile(profileId, requestNull);
        assertThat(profile.getTags()).isEmpty();

        var requestEmpty = new com.paginass.dto.BusinessProfileUpdateRequestDto(
                "Name", null, null, null, null, List.of(), false, null);
        service.updateProfile(profileId, requestEmpty);
        assertThat(profile.getTags()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private BusinessProfile unclaimedProfile() {
        BusinessProfile p = new BusinessProfile();
        p.setId(UUID.randomUUID());
        p.setBizType(BusinessType.INFORMAL);
        p.setName("Test Business");
        p.setSlug("test-business");
        return p;
    }

    private User userWithRole(UserRole role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("test@example.com");
        u.setRole(role);
        return u;
    }

    private BusinessProfileEdit editRecord(UUID businessId, UUID editorId, OffsetDateTime editedAt) {
        BusinessProfileEdit e = new BusinessProfileEdit();
        e.setId(UUID.randomUUID());
        e.setBusinessProfileId(businessId);
        e.setEditedBy(editorId);
        e.setSnapshot(Map.of("name", "Test", "deliversToHome", false));
        e.setEditedAt(editedAt);
        return e;
    }
}
