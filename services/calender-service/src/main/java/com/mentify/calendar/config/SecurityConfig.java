package com.mentify.calendar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Reuses the shared JWT provider resource-server configuration used by Mentify
 * business services.
 */
@Configuration
@Import({
        com.mentify.common.security.SecurityConfig.class,
        com.mentify.common.security.JwtAuthenticationConverter.class,
        com.mentify.common.security.JwtRoleConverter.class
})
public class SecurityConfig {
}
