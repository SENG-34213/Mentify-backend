package com.mentify.analytics.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Reuses the shared Mentify JWT resource-server and role-conversion setup.
 */
@Configuration
@Import({
        com.mentify.common.security.SecurityConfig.class,
        com.mentify.common.security.JwtAuthenticationConverter.class,
        com.mentify.common.security.JwtRoleConverter.class
})
public class SecurityConfig {
}
