package com.mentify.config;

import com.mentify.common.security.JwtAudienceValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Keeps enrollment-service token validation explicit and aligned with the
 * tokens issued by auth-service. This avoids falling back to a stale decoder
 * when configuration is supplied through the optional config server.
 */
@Configuration
public class EnrollmentSecurityConfig {

    @Bean(name = "jwtDecoder")
    @Primary
    JwtDecoder enrollmentJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:8085/api/v1/auth/.well-known/jwks.json}")
            String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8085}")
            String issuer,
            @Value("${mentify.security.audience:mentify-api}") String audience
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtAudienceValidator(audience)
        ));
        return decoder;
    }
}
