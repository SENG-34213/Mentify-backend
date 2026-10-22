package com.mentify.analytics.controller;

import com.mentify.analytics.dto.AdminAnalyticsOverviewResponse;
import com.mentify.analytics.service.AdminAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AdminAnalyticsService adminAnalyticsService;

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/admin/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsStudentRequests() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/admin/overview")
                        .with(jwt().authorities(AuthorityUtils.createAuthorityList("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminRequestsAndReturnsOverview() throws Exception {
        when(adminAnalyticsService.getOverview()).thenReturn(
                new AdminAnalyticsOverviewResponse(null, null, null, null, null, null, null, null, false, java.util.List.of()));

        mockMvc.perform(get("/api/v1/analytics/admin/overview")
                        .with(jwt().authorities(AuthorityUtils.createAuthorityList("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partial").value(false))
                .andExpect(jsonPath("$.unavailableSections").isEmpty());
    }
}
