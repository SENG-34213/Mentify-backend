package com.mentify.service.authentication;

import com.mentify.enums.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class Slf4jAuthenticationEventLogger implements AuthenticationEventLogger {

    @Override
    public void loginSucceeded(UUID localUserId, String keycloakUserId, Role role) {
        log.info(
                "Authentication event timestamp={} event=LOGIN_SUCCEEDED userId={} keycloakUserId={} role={}",
                Instant.now(),
                localUserId,
                keycloakUserId,
                role
        );
    }

    @Override
    public void loginFailed(String reason, String identifier, String keycloakUserId) {
        log.warn(
                "Authentication event timestamp={} event=LOGIN_FAILED reason={} identifier={} keycloakUserId={}",
                Instant.now(),
                reason,
                identifier,
                keycloakUserId
        );
    }

    @Override
    public void passwordResetRequested(UUID localUserId, String keycloakUserId) {
        log.info(
                "Authentication event timestamp={} event=PASSWORD_RESET_EMAIL_REQUESTED userId={} keycloakUserId={}",
                Instant.now(),
                localUserId,
                keycloakUserId
        );
    }

    @Override
    public void passwordResetRequestSkipped(String reason) {
        log.info(
                "Authentication event timestamp={} event=PASSWORD_RESET_REQUEST_SKIPPED reason={}",
                Instant.now(),
                reason
        );
    }

    @Override
    public void passwordResetRequestFailed(UUID localUserId, String keycloakUserId) {
        log.warn(
                "Authentication event timestamp={} event=PASSWORD_RESET_EMAIL_FAILED userId={} keycloakUserId={}",
                Instant.now(),
                localUserId,
                keycloakUserId
        );
    }
}
