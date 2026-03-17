package com.paginass.security;

import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Normalizes provider-specific OAuth2 attribute keys into a common structure.
 * Handles differences between Google, GitHub, and Facebook attribute naming.
 */
public class OAuth2UserInfoExtractor {

    public static String extractEmail(String provider, OAuth2User oAuth2User) {
        return switch (provider.toLowerCase()) {
            case "google", "facebook" -> oAuth2User.getAttribute("email");
            case "github" -> {
                String email = oAuth2User.getAttribute("email");
                // GitHub may return null if the user's email is private
                yield email != null ? email : "";
            }
            default -> oAuth2User.getAttribute("email");
        };
    }

    public static String extractName(String provider, OAuth2User oAuth2User) {
        return switch (provider.toLowerCase()) {
            case "google", "facebook", "github" -> oAuth2User.getAttribute("name");
            default -> oAuth2User.getAttribute("name");
        };
    }

    public static String extractOauthId(String provider, OAuth2User oAuth2User) {
        return switch (provider.toLowerCase()) {
            case "google" -> oAuth2User.getAttribute("sub");
            case "github" -> {
                Integer id = oAuth2User.getAttribute("id");
                yield id != null ? id.toString() : null;
            }
            case "facebook" -> oAuth2User.getAttribute("id");
            default -> oAuth2User.getName();
        };
    }

    private OAuth2UserInfoExtractor() {}
}
