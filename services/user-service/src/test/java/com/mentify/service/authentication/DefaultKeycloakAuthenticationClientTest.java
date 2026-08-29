package com.mentify.service.authentication;

import com.mentify.config.KeycloakProperties;
import com.mentify.dto.LoginRequest;
import com.mentify.exception.AuthenticationFailedException;
import com.mentify.exception.InvalidRefreshTokenException;
import com.mentify.exception.KeycloakAuthenticationException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DefaultKeycloakAuthenticationClientTest {

    private static final String TOKEN_ENDPOINT =
            "http://keycloak.test/realms/mentify/protocol/openid-connect/token";
    private static final String LOGOUT_ENDPOINT =
            "http://keycloak.test/realms/mentify/protocol/openid-connect/logout";

    private MockRestServiceServer server;
    private DefaultKeycloakAuthenticationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();

        KeycloakProperties properties = new KeycloakProperties();
        properties.setServerUrl("http://keycloak.test");
        properties.setRealm("mentify");
        properties.setAuthClientId("mentify-backend-client");
        properties.setAuthClientSecret("secret-value");

        client = new DefaultKeycloakAuthenticationClient(restClientBuilder, properties);
    }

    @Test
    void authenticate_whenCredentialsAreValid_returnsTokenResponse() throws Exception {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("grant_type=password")))
                .andExpect(content().string(containsString("username=student%40gmail.com")))
                .andExpect(content().string(containsString("password=Password%40123")))
                .andRespond(withSuccess(tokenResponseJson(), MediaType.APPLICATION_JSON));

        KeycloakAuthenticationResult result = client.authenticate(LoginRequest.builder()
                .identifier("student@gmail.com")
                .password("Password@123")
                .build());

        assertThat(result.getAccessToken()).isNotBlank();
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getKeycloakUserId()).isEqualTo("keycloak-student-id");
        assertThat(result.getRealmRoles()).containsExactly("STUDENT");
        server.verify();
    }

    @Test
    void authenticate_whenCredentialsAreInvalid_returnsGenericAuthenticationFailure() {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\",\"error_description\":\"bad password\"}"));

        assertThatThrownBy(() -> client.authenticate(LoginRequest.builder()
                .identifier("student@gmail.com")
                .password("Password@123")
                .build()))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void refreshAccessToken_whenRefreshTokenIsValid_usesKeycloakRefreshGrant() throws Exception {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("grant_type=refresh_token")))
                .andExpect(content().string(containsString("refresh_token=valid-refresh-token")))
                .andRespond(withSuccess(tokenResponseJson(), MediaType.APPLICATION_JSON));

        KeycloakAuthenticationResult result = client.refreshAccessToken("valid-refresh-token");

        assertThat(result.getAccessToken()).isNotBlank();
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getExpiresIn()).isEqualTo(300L);
        server.verify();
    }

    @Test
    void refreshAccessToken_whenRefreshTokenIsExpired_returnsGenericUnauthorizedFailure() {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\",\"refresh_token\":\"expired-refresh-token\"}"));

        assertThatThrownBy(() -> client.refreshAccessToken("expired-refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    void refreshAccessToken_whenKeycloakFails_returnsControlledServiceFailure() {
        server.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"server_error\"}"));

        assertThatThrownBy(() -> client.refreshAccessToken("valid-refresh-token"))
                .isInstanceOf(KeycloakAuthenticationException.class)
                .hasMessage("Authentication service is unavailable");
    }

    @Test
    void logout_whenRefreshTokenIsValid_callsKeycloakLogoutEndpoint() {
        server.expect(requestTo(LOGOUT_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("refresh_token=valid-refresh-token")))
                .andRespond(withSuccess());

        client.logout("valid-refresh-token");

        server.verify();
    }

    @Test
    void logout_whenRefreshTokenIsInvalid_returnsGenericUnauthorizedFailure() {
        server.expect(requestTo(LOGOUT_ENDPOINT))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\",\"refresh_token\":\"invalid-refresh-token\"}"));

        assertThatThrownBy(() -> client.logout("invalid-refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    private String tokenResponseJson() throws Exception {
        return """
                {
                  "access_token": "%s",
                  "refresh_token": "refresh-token",
                  "token_type": "Bearer",
                  "expires_in": 300,
                  "refresh_expires_in": 1800,
                  "scope": "openid email profile"
                }
                """.formatted(accessToken());
    }

    private String accessToken() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("keycloak-student-id")
                .claim("email", "student@gmail.com")
                .claim("realm_access", Map.of("roles", List.of("STUDENT")))
                .build();
        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner("01234567890123456789012345678901"));
        return signedJwt.serialize();
    }
}
