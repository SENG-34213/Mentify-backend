package com.mentify.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public final class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error(
            "invalid_token", "The required audience is missing", null
    );

    private final String requiredAudience;

    public JwtAudienceValidator(String requiredAudience) {
        this.requiredAudience = requiredAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<String> audience = token.getAudience();
        return audience != null && audience.contains(requiredAudience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
    }
}
