package com.paginass.config;

import com.paginass.security.CustomOAuth2UserService;
import com.paginass.security.CustomUserDetailsService;
import com.paginass.service.SessionExpiryReasonStore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfigurationSource;

import static com.paginass.security.CustomUserDetailsService.ACCOUNT_DISABLED_BY_ADMIN;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomUserDetailsService customUserDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final SessionExpiryReasonStore sessionExpiryReasonStore;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final String[] PUBLIC_PATHS = {
        "/", "/error",
        "/login**", "/oauth2/**",
        "/uploads/**",
        "/api/public/**",
        "/api/search",
        "/api/auth/register",
        "/api/auth/verify-email",
        "/api/auth/resend-verification",
        "/api/auth/reset-password/confirm"
    };

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) ->
                response.setStatus(HttpServletResponse.SC_OK);
    }

    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, exception) -> {
            response.setContentType("application/json");

            boolean isAdminDisabled = exception instanceof InternalAuthenticationServiceException
                    && ACCOUNT_DISABLED_BY_ADMIN.equals(exception.getMessage());
            boolean isEmailUnverified = exception instanceof DisabledException;

            if (isAdminDisabled) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"reason\":\"disabled\"}");
            } else if (isEmailUnverified) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"reason\":\"unverified\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        };
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            .authenticationProvider(daoAuthenticationProvider())

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/profile/businesses").hasAnyRole("OWNER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/profile/setup").hasAnyRole("EDITOR", "ADMIN")
                .anyRequest().authenticated()
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(unauthorizedEntryPoint())
            )

            .sessionManagement(sessions -> {
                sessions
                    .maximumSessions(1)
                    .sessionRegistry(sessionRegistry())
                    .expiredSessionStrategy(event -> {
                        String sessionId = event.getSessionInformation().getSessionId();
                        String reason = sessionExpiryReasonStore.consumeReason(sessionId);
                        if (reason == null) reason = "concurrent_login";
                        event.getResponse().setContentType("application/json");
                        event.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        event.getResponse().getWriter()
                            .write("{\"error\":\"session_expired\",\"reason\":\"" + reason + "\"}");
                    });
            })

            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler(loginSuccessHandler())
                .failureHandler(loginFailureHandler())
                .permitAll()
            )

            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl(frontendUrl, true)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            )

            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl(frontendUrl)
                .invalidateHttpSession(true)
                .deleteCookies("SESSION")
                .permitAll()
            );

        return http.build();
    }
}
