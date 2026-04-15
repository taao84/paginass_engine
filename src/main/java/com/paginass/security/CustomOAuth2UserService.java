package com.paginass.security;

import com.paginass.entity.User;
import com.paginass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        String email    = OAuth2UserInfoExtractor.extractEmail(provider, oAuth2User);
        String name     = OAuth2UserInfoExtractor.extractName(provider, oAuth2User);
        String oauthId  = OAuth2UserInfoExtractor.extractOauthId(provider, oAuth2User);

        upsertUser(provider, oauthId, email, name);

        return new DefaultOAuth2User(
                Collections.emptyList(),
                oAuth2User.getAttributes(),
                userNameAttributeName
        );
    }

    private void upsertUser(String provider, String oauthId, String email, String name) {
        User user = userRepository
                .findByOauthProviderAndOauthId(provider, oauthId)
                .orElseGet(() -> userRepository.findByEmail(email).orElse(new User()));

        user.setOauthProvider(provider);
        user.setOauthId(oauthId);
        user.setEmail(email);
        user.setFullName(name);
        user.setEmailVerified(true);

        userRepository.save(user);
    }
}
