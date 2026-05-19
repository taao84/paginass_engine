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
import com.paginass.entity.UserRole;
import com.paginass.repository.AppNewsRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppNewsServiceTest {

    @Mock private AppNewsRepository appNewsRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private AppNewsService appNewsService;

    private User admin;
    private AppNews sampleNews;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setEmail("admin@example.com");
        admin.setFullName("Admin User");
        admin.setRole(UserRole.ADMIN);

        sampleNews = new AppNews();
        sampleNews.setId(UUID.randomUUID());
        sampleNews.setTitle("Test Title");
        sampleNews.setDescription("Test description");
        sampleNews.setCreatedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).minusDays(1));
        sampleNews.setCreatedById(admin.getId());

        UserDetails principal = new org.springframework.security.core.userdetails.User(
                "admin@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findAllById(any())).thenReturn(List.of(admin));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getPublished_returnsPagedPosts() {
        when(appNewsRepository.findAllPaged(eq(10), eq(0))).thenReturn(List.of(sampleNews));
        when(appNewsRepository.count()).thenReturn(1L);

        AppNewsPageDto result = appNewsService.getPublished(0);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).title()).isEqualTo("Test Title");
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void getAll_returnsAllPostsWithCreatorName() {
        when(appNewsRepository.findAllPaged(eq(20), eq(0))).thenReturn(List.of(sampleNews));
        when(appNewsRepository.count()).thenReturn(1L);

        AppNewsAdminPageDto result = appNewsService.getAll(0);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).createdByName()).isEqualTo("Admin User");
    }

    @Test
    void create_withoutImage_savesNewsWithNullImageUrl() throws IOException {
        CreateAppNewsRequestDto request = new CreateAppNewsRequestDto("Title", "Description");

        when(appNewsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppNewsAdminDto result = appNewsService.create(request, null);

        assertThat(result.title()).isEqualTo("Title");
        assertThat(result.imageUrl()).isNull();
        verify(storageService, never()).saveFile(anyString(), any(), anyString());
    }

    @Test
    void create_withImage_savesFileAndStoresSubPath() throws IOException {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", new byte[100]);

        CreateAppNewsRequestDto request = new CreateAppNewsRequestDto("Title", "Desc");

        when(appNewsRepository.save(any())).thenAnswer(inv -> {
            AppNews saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(storageService.saveFile(anyString(), any(), eq("image/jpeg"))).thenReturn("news/uuid.jpg");
        when(storageService.toPublicUrl(anyString())).thenReturn("https://cdn.example.com/news/uuid.jpg");

        AppNewsAdminDto result = appNewsService.create(request, image);

        ArgumentCaptor<String> subPathCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService).saveFile(subPathCaptor.capture(), any(), eq("image/jpeg"));
        assertThat(subPathCaptor.getValue()).startsWith("news/");
        assertThat(result.imageUrl()).isEqualTo("https://cdn.example.com/news/uuid.jpg");
    }

    @Test
    void create_withOversizedImage_throwsBadRequest() {
        byte[] bigFile = new byte[(int) (StorageConstants.MAX_NEWS_IMAGE_BYTES + 1)];
        MockMultipartFile image = new MockMultipartFile("image", "big.jpg", "image/jpeg", bigFile);

        CreateAppNewsRequestDto request = new CreateAppNewsRequestDto("Title", "Desc");

        assertThatThrownBy(() -> appNewsService.create(request, image))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_withNewImage_replacesOldImage() throws IOException {
        sampleNews.setImageUrl("news/old.jpg");
        when(appNewsRepository.findById(sampleNews.getId())).thenReturn(Optional.of(sampleNews));
        when(appNewsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.toPublicUrl(anyString())).thenReturn("https://cdn.example.com/news/new.jpg");

        MockMultipartFile newImage = new MockMultipartFile(
                "image", "new.jpg", "image/jpeg", new byte[200]);

        UpdateAppNewsRequestDto request = new UpdateAppNewsRequestDto("Updated", "Updated desc", false);

        appNewsService.update(sampleNews.getId(), request, newImage);

        verify(storageService).deleteFile("news/old.jpg");
        verify(storageService).saveFile(anyString(), any(), eq("image/jpeg"));
    }

    @Test
    void update_withRemoveImage_deletesImageAndClearsUrl() throws IOException {
        sampleNews.setImageUrl("news/old.jpg");
        when(appNewsRepository.findById(sampleNews.getId())).thenReturn(Optional.of(sampleNews));
        when(appNewsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateAppNewsRequestDto request = new UpdateAppNewsRequestDto("Title", "Desc", true);

        AppNewsAdminDto result = appNewsService.update(sampleNews.getId(), request, null);

        verify(storageService).deleteFile("news/old.jpg");
        assertThat(result.imageUrl()).isNull();
    }

    @Test
    void delete_removesImageAndRecord() throws IOException {
        sampleNews.setImageUrl("news/img.jpg");
        when(appNewsRepository.findById(sampleNews.getId())).thenReturn(Optional.of(sampleNews));

        appNewsService.delete(sampleNews.getId());

        verify(storageService).deleteFile("news/img.jpg");
        verify(appNewsRepository).delete(sampleNews);
    }

    @Test
    void delete_nonExistent_throwsNotFound() {
        UUID randomId = UUID.randomUUID();
        when(appNewsRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appNewsService.delete(randomId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
