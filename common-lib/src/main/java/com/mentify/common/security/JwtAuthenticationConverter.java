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
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JwtRoleConverter roleConverter;
    @Value("${mentify.security.principal-claim:preferred_username}") private String principalClaim = "preferred_username";
    @Override public AbstractAuthenticationToken convert(Jwt jwt) {
        String principal = jwt.hasClaim(principalClaim) ? jwt.getClaimAsString(principalClaim) : null;
        return new JwtAuthenticationToken(jwt, roleConverter.convert(jwt),
                principal == null || principal.isBlank() ? jwt.getSubject() : principal);
    }
}
