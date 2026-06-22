package com.mentify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.common.security.KeycloakJwtAuthenticationConverter;
import com.mentify.common.security.KeycloakRoleConverter;
import com.mentify.common.security.SecurityConfig;
import com.mentify.dto.LoginRequest;
import com.mentify.dto.LoginResponse;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import com.mentify.exception.AccountAccessDeniedException;
import com.mentify.exception.AuthenticationFailedException;
import com.mentify.exception.KeycloakAuthenticationException;
import com.mentify.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserLoginController.class,
        properties = {
                "spring.cloud.config.enabled=false",
                "mentify.security.enabled=true"
        }
)
@Import({SecurityConfig.class, KeycloakJwtAuthenticationConverter.class, KeycloakRoleConverter.class})
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void login_whenRequestIsValid_returnsStandardSuccessResponse() throws Exception {
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(loginResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.user.email").value("student@gmail.com"))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.clientSecret").doesNotExist());

        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    void login_whenPasswordIsWrong_returnsGenericUnauthorized() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationFailedException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.message").value(not("User not found")));
    }

    @Test
    void login_whenRequestHasMissingFields_returnsFieldValidationErrorsAndDoesNotCallService() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .identifier("")
                .password(" ")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.fieldErrors.identifier").exists())
                .andExpect(jsonPath("$.fieldErrors.password").value("Password is required"));

        verifyNoInteractions(authenticationService);
    }

    @Test
    void login_whenIdentifierIsNotEmail_returnsFieldValidationErrorAndDoesNotCallService() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .identifier("not-an-email")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.identifier").value("Login identifier must be a valid email address"));

        verifyNoInteractions(authenticationService);
    }

    @Test
    void login_whenAccountIsInactive_returnsForbiddenWithControlledMessage() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new AccountAccessDeniedException("Account is not allowed to access the platform"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("Account is not allowed to access the platform"));
    }

    @Test
    void login_whenAccountIsLocked_returnsForbiddenWithLockedMessage() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new AccountAccessDeniedException("Your account is locked"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("Your account is locked"));
    }

    @Test
    void login_whenKeycloakUnavailable_returnsControlledServiceError() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new KeycloakAuthenticationException("connection refused: http://internal"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.statusCode").value(503))
                .andExpect(jsonPath("$.message").value("Authentication service is unavailable"));
    }

    @Test
    void login_whenSuccessfulResponseIsSerialized_doesNotExposeSecretFields() throws Exception {
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(loginResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user", not(hasKey("password"))))
                .andExpect(jsonPath("$.data", not(hasKey("clientSecret"))));
    }

    private LoginRequest loginRequest() {
        return LoginRequest.builder()
                .identifier("student@gmail.com")
                .password("Password@123")
                .build();
    }

    private LoginResponse loginResponse() {
        return LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(300L)
                .refreshExpiresIn(1800L)
                .scope("openid email profile")
                .issuedAt(Instant.now())
                .user(LoginResponse.UserSummary.builder()
                        .id(UUID.randomUUID())
                        .keycloakUserId("keycloak-student-id")
                        .email("student@gmail.com")
                        .firstName("Student")
                        .lastName("User")
                        .role(Role.STUDENT)
                        .accountStatus(AccountStatus.ACTIVE)
                        .emailVerified(true)
                        .profileComplete(true)
                        .build())
                .build();
    }
}
