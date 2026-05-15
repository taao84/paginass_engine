package com.paginass.service;

import com.paginass.config.EntityType;
import com.paginass.config.EventType;
import com.paginass.dto.BusinessLikeDto;
import com.paginass.entity.BusinessLike;
import com.paginass.entity.User;
import com.paginass.repository.BusinessLikeRepository;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessLikeService {

    private final BusinessLikeRepository businessLikeRepository;
    private final UserActivityEventService userActivityEventService;
    private final UserRepository userRepository;

    @Transactional
    public BusinessLikeDto toggleLike(UUID businessId) {
        User user = resolveCurrentUser();
        boolean currentlyLiked = businessLikeRepository.existsByUserIdAndBusinessId(user.getId(), businessId);

        if (currentlyLiked) {
            businessLikeRepository.deleteByUserIdAndBusinessId(user.getId(), businessId);
            userActivityEventService.record(user.getId(), EventType.UNLIKE_BUSINESS, EntityType.BUSINESS, businessId, null);
        } else {
            BusinessLike like = new BusinessLike();
            like.setUserId(user.getId());
            like.setBusinessId(businessId);
            businessLikeRepository.save(like);
            userActivityEventService.record(user.getId(), EventType.LIKE_BUSINESS, EntityType.BUSINESS, businessId, null);
        }

        long newCount = businessLikeRepository.countByBusinessId(businessId);
        return new BusinessLikeDto(!currentlyLiked, newCount);
    }

    @Transactional(readOnly = true)
    public BusinessLikeDto getLikeStatus(UUID businessId) {
        User user = resolveCurrentUser();
        boolean liked = businessLikeRepository.existsByUserIdAndBusinessId(user.getId(), businessId);
        long count = businessLikeRepository.countByBusinessId(businessId);
        return new BusinessLikeDto(liked, count);
    }

    @Transactional(readOnly = true)
    public long getLikeCount(UUID businessId) {
        return businessLikeRepository.countByBusinessId(businessId);
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
