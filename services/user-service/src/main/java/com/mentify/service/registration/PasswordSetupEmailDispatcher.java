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
    private final TemporaryCredentialsEmailDeliveryService temporaryCredentialsEmailDeliveryService;

    public void sendAfterCommit(String keycloakUserId, String email, String firstName, String temporaryPassword) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendSafely(keycloakUserId, email, firstName, temporaryPassword);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendSafely(keycloakUserId, email, firstName, temporaryPassword);
            }
        });
    }

    private void sendSafely(String keycloakUserId, String email, String firstName, String temporaryPassword) {
        try {
            keycloakUserService.sendPasswordSetupEmail(keycloakUserId);
        } catch (Exception exception) {
            log.error("Failed to send Keycloak password setup email for user {}. Registration remains committed; use resend invitation.",
                    keycloakUserId,
                    exception);
        }
        try {
            temporaryCredentialsEmailDeliveryService.send(email, firstName, temporaryPassword);
        } catch (Exception exception) {
            // Never log the temporary credential or a mail exception that might contain message content.
            log.error("Failed to deliver temporary sign-in credentials for Keycloak user {}", keycloakUserId);
        }
    }
}
