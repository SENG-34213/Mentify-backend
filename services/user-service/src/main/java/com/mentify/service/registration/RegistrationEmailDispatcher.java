package com.mentify.service.registration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.mentify.enums.Role;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationEmailDispatcher {

    private final TemporaryCredentialsEmailDeliveryService temporaryCredentialsEmailDeliveryService;

    public void sendAfterCommit(String authUserId, String email, String firstName, String temporaryPassword) {
        sendAfterCommit(authUserId, email, firstName, temporaryPassword, null);
    }

    public void sendAfterCommit(String authUserId, String email, String firstName, String temporaryPassword, Role role) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendSafely(authUserId, email, firstName, temporaryPassword, role);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    sendSafely(authUserId, email, firstName, temporaryPassword, role);
                } catch (RegistrationEmailDeliveryException exception) {
                    // The user transaction has already committed. Do not turn a
                    // successful registration into an HTTP 500 because SMTP is
                    // temporarily unavailable. The failure is logged for retry/
                    // operational follow-up instead.
                    log.error("Registration completed but credentials email could not be delivered for auth user {}",
                            authUserId, exception);
                }
            }
        });
    }

    private void sendSafely(String authUserId, String email, String firstName, String temporaryPassword, Role role) {
        try {
            if (role == null) {
                temporaryCredentialsEmailDeliveryService.send(email, firstName, temporaryPassword);
            } else {
                temporaryCredentialsEmailDeliveryService.send(email, firstName, temporaryPassword, role);
            }
        } catch (Exception exception) {
            // Never log the temporary credential or a mail exception that might contain message content.
            log.error("Failed to deliver sign-in credentials for auth user {}", authUserId);
            throw new RegistrationEmailDeliveryException();
        }
    }
}
