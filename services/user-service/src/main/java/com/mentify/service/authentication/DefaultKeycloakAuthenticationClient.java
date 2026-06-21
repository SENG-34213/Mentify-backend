package com.mentify.service.authentication;

import com.mentify.config.KeycloakProperties;
import com.mentify.dto.LoginRequest;
import com.mentify.exception.AuthenticationFailedException;
import com.mentify.exception.KeycloakAuthenticationException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DefaultKeycloakAuthenticationClient implements KeycloakAuthenticationClient {

    private static final String PASSWORD_GRANT_TYPE = "password";

    private final RestClient.Builder restClientBuilder;
    private final KeycloakProperties keycloakProperties;

    @Override
    public KeycloakAuthenticationResult authenticate(LoginRequest request) {
        MultiValueMap<String, String> form = buildTokenRequest(request);

        try {
            KeycloakTokenResponse tokenResponse = restClientBuilder.build()
                    .post()
                    .uri(tokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.BAD_REQUEST || status == HttpStatus.UNAUTHORIZED,
                            (clientRequest, clientResponse) -> {
                                throw new AuthenticationFailedException("Invalid email or password");
                            })
                    .onStatus(status -> status == HttpStatus.FORBIDDEN,
                            (clientRequest, clientResponse) -> {
                                throw new AuthenticationFailedException("Invalid email or password");
                            })
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (clientRequest, clientResponse) -> {
                                throw new KeycloakAuthenticationException("Authentication service is unavailable");
                            })
                    .body(KeycloakTokenResponse.class);

            return toAuthenticationResult(tokenResponse);
        } catch (AuthenticationFailedException | KeycloakAuthenticationException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new KeycloakAuthenticationException("Authentication service is unavailable", exception);
        }
    }

    private MultiValueMap<String, String> buildTokenRequest(LoginRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", PASSWORD_GRANT_TYPE);
        form.add("client_id", keycloakProperties.getAuthClientId());
        if (keycloakProperties.getAuthClientSecret() != null && !keycloakProperties.getAuthClientSecret().isBlank()) {
            form.add("client_secret", keycloakProperties.getAuthClientSecret());
        }
        form.add("username", request.getIdentifier().trim());
        form.add("password", request.getPassword());
        return form;
    }

    private KeycloakAuthenticationResult toAuthenticationResult(KeycloakTokenResponse tokenResponse) {
        if (tokenResponse == null || tokenResponse.getAccessToken() == null || tokenResponse.getAccessToken().isBlank()) {
            throw new KeycloakAuthenticationException("Authentication service returned an invalid response");
        }

        JWTClaimsSet claims = parseClaims(tokenResponse.getAccessToken());

        return KeycloakAuthenticationResult.builder()
                .keycloakUserId(claims.getSubject())
                .email(extractEmail(claims))
                .realmRoles(extractRealmRoles(claims))
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .tokenType(tokenResponse.getTokenType())
                .expiresIn(tokenResponse.getExpiresIn())
                .refreshExpiresIn(tokenResponse.getRefreshExpiresIn())
                .scope(tokenResponse.getScope())
                .build();
    }

    private JWTClaimsSet parseClaims(String accessToken) {
        try {
            return SignedJWT.parse(accessToken).getJWTClaimsSet();
        } catch (ParseException exception) {
            throw new KeycloakAuthenticationException("Authentication service returned an invalid response", exception);
        }
    }

    private String extractEmail(JWTClaimsSet claims) {
        try {
            String email = claims.getStringClaim("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
            return claims.getStringClaim("preferred_username");
        } catch (ParseException exception) {
            throw new KeycloakAuthenticationException("Authentication service returned an invalid response", exception);
        }
    }

    private Set<String> extractRealmRoles(JWTClaimsSet claims) {
        Object realmAccessClaim = claims.getClaim("realm_access");
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return Collections.emptySet();
        }

        Object rolesClaim = realmAccess.get("roles");
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return Collections.emptySet();
        }

        Set<String> normalizedRoles = new LinkedHashSet<>();
        roles.stream()
                .map(Object::toString)
                .map(this::normalizeRoleName)
                .filter(role -> !role.isBlank())
                .forEach(normalizedRoles::add);
        return normalizedRoles;
    }

    private String normalizeRoleName(String roleName) {
        return roleName.trim().replace('-', '_').toUpperCase();
    }

    private String tokenEndpoint() {
        return UriComponentsBuilder
                .fromHttpUrl(keycloakProperties.getServerUrl())
                .pathSegment("realms", keycloakProperties.getRealm(), "protocol", "openid-connect", "token")
                .toUriString();
    }
}
