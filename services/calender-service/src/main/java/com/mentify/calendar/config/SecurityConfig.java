package com.mentify.calendar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Reuses the shared Keycloak resource-server configuration used by Mentify
 * business services.
 */
@Configuration
@Import({
        com.mentify.common.security.SecurityConfig.class,
        com.mentify.common.security.KeycloakJwtAuthenticationConverter.class,
        com.mentify.common.security.KeycloakRoleConverter.class
})
public class SecurityConfig {
}
