package com.mentify.communication.controller;

import com.mentify.communication.config.SecurityConfig;
import com.mentify.communication.security.AuthenticatedUserService;
import com.mentify.communication.TestJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CommunicationTestController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "mentify.security.enabled=true"
        }
)
@Import({SecurityConfig.class, AuthenticatedUserService.class, TestJwtDecoderConfig.class})
class CommunicationTestControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenNoToken_whenCallingMeEndpoint_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/communication/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenValidJwt_whenCallingMeEndpoint_thenReturnsCurrentUserId() throws Exception {
        mockMvc.perform(get("/api/communication/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(USER_ID.toString())
                                .claim("preferred_username", "student@mentify.com")
                                .claim("realm_access", java.util.Map.of(
                                        "roles", java.util.List.of("STUDENT")
                                )))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));
    }
}
