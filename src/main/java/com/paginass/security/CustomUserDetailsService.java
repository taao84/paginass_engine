package com.paginass.security;

import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public static final String  ACCOUNT_DISABLED_BY_ADMIN = "ACCOUNT_DISABLED_BY_ADMIN";

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.paginass.entity.User user = userRepository.findByEmail(email)
                .filter(u -> u.getPasswordHash() != null)
                .orElseThrow(() -> new UsernameNotFoundException("No local account found for: " + email));

        if (!user.isEnabled()) {
            throw new DisabledException(ACCOUNT_DISABLED_BY_ADMIN);
        }

        return User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!user.isEmailVerified())
                .build();
    }
}
