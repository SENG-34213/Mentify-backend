package com.mentify.communication.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Imports the shared AuditorAware implementation that reads the Keycloak sub claim.
 */
@Configuration
@Import(com.mentify.config.JpaAuditingConfig.class)
public class JpaAuditingConfig {
}
