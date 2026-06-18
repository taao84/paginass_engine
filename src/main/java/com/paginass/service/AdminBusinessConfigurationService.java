package com.paginass.service;

import com.paginass.dto.BusinessConfigurationDto;
import com.paginass.entity.BusinessConfiguration;
import com.paginass.entity.User;
import com.paginass.repository.BusinessConfigurationRepository;
import com.paginass.repository.BusinessProfileRepository;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminBusinessConfigurationService {

    private final BusinessConfigurationRepository businessConfigurationRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public BusinessConfigurationDto getConfiguration(UUID businessId) {
        if (!businessProfileRepository.existsById(businessId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        }
        return businessConfigurationRepository.findById(businessId)
                .map(config -> new BusinessConfigurationDto(config.getMaxFeaturedProducts()))
                .orElse(new BusinessConfigurationDto(null));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public BusinessConfigurationDto updateConfiguration(UUID businessId, BusinessConfigurationDto request) {
        if (!businessProfileRepository.existsById(businessId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        }
        User admin = resolveCurrentUser();
        BusinessConfiguration config = businessConfigurationRepository.findById(businessId)
                .orElseGet(() -> {
                    BusinessConfiguration newConfig = new BusinessConfiguration();
                    newConfig.setBusinessId(businessId);
                    return newConfig;
                });
        config.setMaxFeaturedProducts(request.maxFeaturedProducts());
        config.setUpdatedBy(admin.getId());
        businessConfigurationRepository.save(config);
        return new BusinessConfigurationDto(config.getMaxFeaturedProducts());
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
