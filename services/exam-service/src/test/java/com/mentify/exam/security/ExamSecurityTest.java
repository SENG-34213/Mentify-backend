package com.mentify.exam.security;

import com.mentify.exam.support.SecurityProbeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {com.mentify.ExamServiceApplication.class, SecurityProbeController.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExamSecurityTest {

    private static final String WHOAMI = "/api/v1/exams/probe/whoami";
    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        Jwt valid = Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .subject(USER_ID.toString())
                .claim("preferred_username", "teacher1")
                .claim("realm_access", Map.of("roles", List.of("TEACHER", "ADMIN", "offline_access")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(jwtDecoder.decode("valid-token")).thenReturn(valid);
        when(jwtDecoder.decode("expired-token")).thenThrow(new BadJwtException("Jwt expired"));
        when(jwtDecoder.decode("invalid-token")).thenThrow(new BadJwtException("Invalid signature"));
    }

    @Test
    void validJwtIsAuthenticatedAndIdentityAndRolesComeFromToken() throws Exception {
        mockMvc.perform(get(WHOAMI).header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.roles[1]").value("TEACHER"))
                .andExpect(jsonPath("$.roles.length()").value(2));
    }

    @Test
    void missingTokenIsRejected() throws Exception {
        mockMvc.perform(get(WHOAMI)).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mockMvc.perform(get(WHOAMI).header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        mockMvc.perform(get(WHOAMI).header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpointIsPublicAndUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void unknownTokenValueNeverReachesController() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("bad"));
        mockMvc.perform(get(WHOAMI).header("Authorization", "Bearer other"))
                .andExpect(status().isUnauthorized());
    }
}
