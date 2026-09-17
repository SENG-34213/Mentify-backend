package com.mentify.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import({
        com.mentify.common.security.KeycloakJwtAuthenticationConverter.class,
        com.mentify.common.security.KeycloakRoleConverter.class
})
public class SecurityConfig {

    @org.springframework.context.annotation.Bean
    public org.springframework.security.web.SecurityFilterChain aiSecurityFilterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http,
                                                                                      org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt, org.springframework.security.authentication.AbstractAuthenticationToken> keycloakJwtAuthenticationConverter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/ai/health").permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter))
                )
                .build();
    }
}
