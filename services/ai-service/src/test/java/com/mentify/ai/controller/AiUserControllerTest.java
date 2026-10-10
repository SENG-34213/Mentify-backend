package com.mentify.ai.controller;

import com.mentify.ai.config.SecurityConfig;
import com.mentify.ai.security.AuthenticatedUserService;
import com.mentify.common.security.KeycloakJwtAuthenticationConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiUserController.class, properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AiUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @MockBean
    private AuthenticatedUserService authenticatedUserService;

    @Test
    void meEndpointShouldBeProtected() throws Exception {
        mockMvc.perform(get("/api/ai/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void meEndpointShouldReturnUserInfoWhenAuthenticated() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authenticatedUserService.getCurrentUserId()).thenReturn(userId);
        when(authenticatedUserService.getCurrentUserRoles()).thenReturn(Set.of("STUDENT"));

        mockMvc.perform(get("/api/ai/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));
    }
}
