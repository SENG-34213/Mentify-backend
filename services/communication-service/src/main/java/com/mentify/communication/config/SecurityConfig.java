package com.mentify.communication.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Imports Mentify's shared Keycloak resource-server configuration.
 */
@Configuration
@Import({
        com.mentify.common.security.SecurityConfig.class,
        com.mentify.common.security.KeycloakJwtAuthenticationConverter.class,
        com.mentify.common.security.KeycloakRoleConverter.class
})
public class SecurityConfig {
}
