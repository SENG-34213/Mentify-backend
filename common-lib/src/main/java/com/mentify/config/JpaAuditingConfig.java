package com.mentify.config;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {


    /**
     * Provides the currently authenticated user's UUID (from Keycloak JWT sub claim)
     * to JPA auditing for @CreatedBy and @LastModifiedBy.
     *
     * @ConditionalOnMissingBean allows any service to override this bean
     * with their own implementation if needed — without causing conflicts.
     */
    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<UUID> auditorAware(){
        return () -> {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || !(authentication.getPrincipal() instanceof Jwt jwt)) {
                return Optional.empty();
            }

            return Optional.of(UUID.fromString(jwt.getSubject()));
        };

    }
}
