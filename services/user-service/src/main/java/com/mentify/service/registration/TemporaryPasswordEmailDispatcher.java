package com.mentify.service.registration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemporaryPasswordEmailDispatcher {

    private final TemporaryPasswordEmailSender emailSender;

    public void send(String recipient, String password) {
        try {
            emailSender.send(recipient, password);
        } catch (MailException exception) {
            throw new TemporaryPasswordEmailException("Temporary password email service is unavailable", exception);
        }
    }

    public void sendAfterCommit(String keycloakUserId, String recipient, String password) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendSafely(keycloakUserId, recipient, password);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendSafely(keycloakUserId, recipient, password);
            }
        });
    }

    private void sendSafely(String keycloakUserId, String recipient, String password) {
        try {
            emailSender.send(recipient, password);
        } catch (MailException exception) {
            log.error(
                    "Failed to send temporary-password email for Keycloak user {}. Registration is committed and invitation may be resent (error type: {})",
                    keycloakUserId,
                    exception.getClass().getSimpleName()
            );
        }
    }
}
