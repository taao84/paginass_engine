package com.paginass.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paginass.entity.BusinessProfile;
import com.paginass.entity.BusinessProfileEdit;
import com.paginass.entity.BusinessType;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.BusinessClaimRepository;
import com.paginass.repository.BusinessProfileEditRepository;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    // -------------------------------------------------------------------------
    // authorizeEdit — unclaimed business
    // -------------------------------------------------------------------------

    @Test
    void authorizeEdit_adminCanEditUnclaimed() {
        BusinessProfile profile = unclaimedProfile();
        User admin = userWithRole(UserRole.ADMIN);
        // should not throw
        service.authorizeEdit(profile, admin);
    }

    @Test
    void authorizeEdit_editorCanEditUnclaimed() {
        BusinessProfile profile = unclaimedProfile();
        User editor = userWithRole(UserRole.EDITOR);
        // should not throw
        service.authorizeEdit(profile, editor);
    }

    @Test
    void authorizeEdit_contributorCannotEditUnclaimed() {
        BusinessProfile profile = unclaimedProfile();
        User contributor = userWithRole(UserRole.CONTRIBUTOR);
        assertThatThrownBy(() -> service.authorizeEdit(profile, contributor))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void authorizeEdit_ownerCannotEditUnclaimedAfterBreq017() {
        BusinessProfile profile = unclaimedProfile();
        User owner = userWithRole(UserRole.OWNER);
        assertThatThrownBy(() -> service.authorizeEdit(profile, owner))
                .isInstanceOf(ResponseStatusException.class);
    }

    // -------------------------------------------------------------------------
    // authorizeEdit — claimed business
    // -------------------------------------------------------------------------

    @Test
    void authorizeEdit_ownerCanEditOwnClaimed() {
        UUID ownerId = UUID.randomUUID();
        BusinessProfile profile = claimedProfile(ownerId);
        User owner = userWithRole(UserRole.OWNER);
        owner.setId(ownerId);
        // should not throw
        service.authorizeEdit(profile, owner);
    }

    @Test
    void authorizeEdit_ownerCannotEditOthersClaimed() {
        BusinessProfile profile = claimedProfile(UUID.randomUUID());
        User owner = userWithRole(UserRole.OWNER);
        owner.setId(UUID.randomUUID()); // different owner
        assertThatThrownBy(() -> service.authorizeEdit(profile, owner))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void authorizeEdit_editorCannotEditClaimed() {
        BusinessProfile profile = claimedProfile(UUID.randomUUID());
        User editor = userWithRole(UserRole.EDITOR);
        assertThatThrownBy(() -> service.authorizeEdit(profile, editor))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void authorizeEdit_adminCanEditClaimed() {
        BusinessProfile profile = claimedProfile(UUID.randomUUID());
        User admin = userWithRole(UserRole.ADMIN);
        // should not throw
        service.authorizeEdit(profile, admin);
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

        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(profile));
        when(businessProfileEditRepository.findById(editId)).thenReturn(Optional.of(edit));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileEditRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(storageService.toPublicUrl(any())).thenAnswer(i -> i.getArgument(0));

        service.restoreFromEdit(businessId, editId, admin);

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

        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(profile));
        when(businessProfileEditRepository.findById(editId)).thenReturn(Optional.of(edit));
        when(businessProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(businessProfileEditRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(storageService.toPublicUrl(any())).thenAnswer(i -> i.getArgument(0));

        service.restoreFromEdit(businessId, editId, admin);

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

        when(businessProfileRepository.findById(businessId)).thenReturn(Optional.of(profile));
        when(businessProfileEditRepository.findById(editId)).thenReturn(Optional.of(edit));

        User admin = userWithRole(UserRole.ADMIN);

        assertThatThrownBy(() -> service.restoreFromEdit(businessId, editId, admin))
                .isInstanceOf(ResponseStatusException.class);
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

    private BusinessProfile claimedProfile(UUID ownerId) {
        BusinessProfile p = unclaimedProfile();
        p.setOwnerId(ownerId);
        p.setClaimed(true);
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
