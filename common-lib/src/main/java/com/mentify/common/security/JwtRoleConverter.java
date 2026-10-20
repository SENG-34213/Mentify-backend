package com.mentify.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnClass(name = "org.springframework.security.oauth2.jwt.Jwt")
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim instanceof Collection<?> roles) return authorities(roles);
        Map<String, Object> access = jwt.getClaimAsMap("realm_access");
        if (access == null || !(access.get("roles") instanceof Collection<?> roles)) return Collections.emptySet();
        return authorities(roles);
    }
    private Collection<GrantedAuthority> authorities(Collection<?> roles) {
        return roles.stream().map(Object::toString).map(String::trim).filter(role -> !role.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }
}
