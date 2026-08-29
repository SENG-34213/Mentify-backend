package com.mentify.service.registration;

import com.mentify.service.KeycloakUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordSetupEmailDispatcher {

    private final KeycloakUserService keycloakUserService;

    public void sendAfterCommit(String keycloakUserId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendSafely(keycloakUserId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendSafely(keycloakUserId);
            }
        });
    }

    private void sendSafely(String keycloakUserId) {
        try {
            keycloakUserService.sendPasswordSetupEmail(keycloakUserId);
        } catch (Exception exception) {
            log.error("Failed to send Keycloak password setup email for user {}. Registration remains committed; use resend invitation.",
                    keycloakUserId,
                    exception);
        }
    }
}
