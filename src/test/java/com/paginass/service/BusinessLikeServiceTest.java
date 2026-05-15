package com.paginass.service;

import com.paginass.dto.BusinessLikeDto;
import com.paginass.entity.BusinessLike;
import com.paginass.entity.User;
import com.paginass.entity.UserRole;
import com.paginass.repository.BusinessLikeRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BusinessLikeServiceTest {

    @Mock private BusinessLikeRepository businessLikeRepository;
    @Mock private UserActivityEventService userActivityEventService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private BusinessLikeService businessLikeService;

    private User contributor;
    private UUID businessId;

    @BeforeEach
    void setup() {
        contributor = new User();
        contributor.setId(UUID.randomUUID());
        contributor.setEmail("user@example.com");
        contributor.setRole(UserRole.CONTRIBUTOR);

        businessId = UUID.randomUUID();

        UserDetails principal = new org.springframework.security.core.userdetails.User(
                "user@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_CONTRIBUTOR")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(contributor));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void toggleLike_whenNotLiked_createsLikeAndRecordsEvent() {
        when(businessLikeRepository.existsByUserIdAndBusinessId(contributor.getId(), businessId)).thenReturn(false);
        when(businessLikeRepository.countByBusinessId(businessId)).thenReturn(1L);

        BusinessLikeDto result = businessLikeService.toggleLike(businessId);

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(1L);

        ArgumentCaptor<BusinessLike> captor = ArgumentCaptor.forClass(BusinessLike.class);
        verify(businessLikeRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(contributor.getId());
        assertThat(captor.getValue().getBusinessId()).isEqualTo(businessId);

        verify(userActivityEventService).record(
                eq(contributor.getId()), eq("LIKE_BUSINESS"), eq("BUSINESS"), eq(businessId), eq(null));
    }

    @Test
    void toggleLike_whenAlreadyLiked_removesLikeAndRecordsEvent() {
        when(businessLikeRepository.existsByUserIdAndBusinessId(contributor.getId(), businessId)).thenReturn(true);
        when(businessLikeRepository.countByBusinessId(businessId)).thenReturn(0L);

        BusinessLikeDto result = businessLikeService.toggleLike(businessId);

        assertThat(result.liked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(0L);

        verify(businessLikeRepository).deleteByUserIdAndBusinessId(contributor.getId(), businessId);
        verify(businessLikeRepository, never()).save(any());

        verify(userActivityEventService).record(
                eq(contributor.getId()), eq("UNLIKE_BUSINESS"), eq("BUSINESS"), eq(businessId), eq(null));
    }

    @Test
    void getLikeStatus_returnsLikedTrueWhenLikeExists() {
        when(businessLikeRepository.existsByUserIdAndBusinessId(contributor.getId(), businessId)).thenReturn(true);
        when(businessLikeRepository.countByBusinessId(businessId)).thenReturn(3L);

        BusinessLikeDto result = businessLikeService.getLikeStatus(businessId);

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(3L);
    }

    @Test
    void getLikeStatus_returnsLikedFalseWhenNoLike() {
        when(businessLikeRepository.existsByUserIdAndBusinessId(contributor.getId(), businessId)).thenReturn(false);
        when(businessLikeRepository.countByBusinessId(businessId)).thenReturn(0L);

        BusinessLikeDto result = businessLikeService.getLikeStatus(businessId);

        assertThat(result.liked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(0L);
    }

    @Test
    void getLikeCount_returnsRepositoryCount() {
        when(businessLikeRepository.countByBusinessId(businessId)).thenReturn(5L);

        long count = businessLikeService.getLikeCount(businessId);

        assertThat(count).isEqualTo(5L);
    }
}
