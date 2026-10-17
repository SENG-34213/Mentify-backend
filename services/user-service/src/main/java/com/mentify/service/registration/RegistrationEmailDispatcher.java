package com.mentify.service.registration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationEmailDispatcher {

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
            temporaryCredentialsEmailDeliveryService.send(email, firstName, temporaryPassword);
        } catch (Exception exception) {
            // Never log the temporary credential or a mail exception that might contain message content.
            log.error("Failed to deliver sign-in credentials for Keycloak user {}", keycloakUserId);
            throw new RegistrationEmailDeliveryException();
        }
    }
}
