package com.paginass.controller;

import com.paginass.config.SecurityConfig;
import com.paginass.dto.BusinessConfigurationDto;
import com.paginass.service.AdminBusinessConfigurationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminBusinessConfigurationController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
class AdminBusinessConfigurationControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated()
                    );
            return http.build();
        }
    }

    private static final String ANY_BUSINESS_ID = "00000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminBusinessConfigurationService adminBusinessConfigurationService;

    // -------------------------------------------------------------------------
    // GET /api/admin/businesses/{id}/configuration
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "OWNER")
    void getConfiguration_forbidsOwner() throws Exception {
        mockMvc.perform(get("/api/admin/businesses/" + ANY_BUSINESS_ID + "/configuration"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CONTRIBUTOR")
    void getConfiguration_forbidsContributor() throws Exception {
        mockMvc.perform(get("/api/admin/businesses/" + ANY_BUSINESS_ID + "/configuration"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getConfiguration_allowsAdmin() throws Exception {
        when(adminBusinessConfigurationService.getConfiguration(any(UUID.class)))
                .thenReturn(new BusinessConfigurationDto(null));

        mockMvc.perform(get("/api/admin/businesses/" + ANY_BUSINESS_ID + "/configuration"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // PUT /api/admin/businesses/{id}/configuration
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "OWNER")
    void updateConfiguration_forbidsOwner() throws Exception {
        mockMvc.perform(put("/api/admin/businesses/" + ANY_BUSINESS_ID + "/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxFeaturedProducts\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CONTRIBUTOR")
    void updateConfiguration_forbidsContributor() throws Exception {
        mockMvc.perform(put("/api/admin/businesses/" + ANY_BUSINESS_ID + "/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxFeaturedProducts\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateConfiguration_allowsAdmin() throws Exception {
        when(adminBusinessConfigurationService.updateConfiguration(any(UUID.class), any(BusinessConfigurationDto.class)))
                .thenReturn(new BusinessConfigurationDto(10));

        mockMvc.perform(put("/api/admin/businesses/" + ANY_BUSINESS_ID + "/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxFeaturedProducts\":10}"))
                .andExpect(status().isOk());
    }
}
