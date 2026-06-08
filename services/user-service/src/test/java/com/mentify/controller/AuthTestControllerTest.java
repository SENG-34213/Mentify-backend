package com.mentify.controller;

import com.mentify.common.security.KeycloakJwtAuthenticationConverter;
import com.mentify.common.security.KeycloakRoleConverter;
import com.mentify.common.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthTestController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "mentify.security.enabled=true"
        }
)
@Import({SecurityConfig.class, KeycloakJwtAuthenticationConverter.class, KeycloakRoleConverter.class})
class AuthTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void publicEndpointShouldReturnOkWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/public-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void protectedEndpointShouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/protected-test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointShouldReturnOkWithValidMockJwt() throws Exception {
        mockMvc.perform(get("/api/v1/auth/protected-test")
                        .with(jwt().jwt(jwt -> jwt
                                .subject("user-123")
                                .claim("email", "admin@mentify.com")
                                .claim("preferred_username", "admin@mentify.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.email").value("admin@mentify.com"))
                .andExpect(jsonPath("$.username").value("admin@mentify.com"));
    }

    @Test
    void adminEndpointShouldReturnOkWithAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin-test")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpointShouldReturnForbiddenWithStudentRole() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admin-test")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentEndpointShouldReturnOkWithStudentRole() throws Exception {
        mockMvc.perform(get("/api/v1/auth/student-test")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk());
    }
}
