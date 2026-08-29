package com.mentify.service.authentication;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class KeycloakAuthenticationResult {
    String keycloakUserId;
    String email;
    Set<String> realmRoles;
    String accessToken;
    String refreshToken;
    String tokenType;
    Long expiresIn;
    Long refreshExpiresIn;
    String scope;
}
