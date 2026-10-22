package com.mentify.communication.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Imports Mentify's shared JWT provider resource-server configuration.
 */
@Configuration
@Import({
        com.mentify.common.security.SecurityConfig.class,
        com.mentify.common.security.JwtAuthenticationConverter.class,
        com.mentify.common.security.JwtRoleConverter.class
})
public class SecurityConfig {
}
