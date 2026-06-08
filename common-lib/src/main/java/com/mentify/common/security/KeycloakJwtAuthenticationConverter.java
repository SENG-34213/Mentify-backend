package com.mentify.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnClass(name = "org.springframework.security.oauth2.jwt.Jwt")
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final KeycloakRoleConverter keycloakRoleConverter;

    @Value("${mentify.security.principal-claim:preferred_username}")
    private String principalClaim;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String principalName = jwt.getClaimAsString(principalClaim);

        if (principalName == null || principalName.isBlank()) {
            principalName = jwt.getSubject();
        }

        return new JwtAuthenticationToken(
                jwt,
                keycloakRoleConverter.convert(jwt),
                principalName
        );
    }
}
