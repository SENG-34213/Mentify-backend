package com.mentify.service.authentication;

import com.mentify.dto.LoginRequest;

public interface KeycloakAuthenticationClient {
    KeycloakAuthenticationResult authenticate(LoginRequest request);
}
