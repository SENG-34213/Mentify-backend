package com.mentify.service.registration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RegistrationEmailDispatcherTest {

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sendAfterCommit_sendsCredentialEmailWithoutSendingJwtProviderActionEmail() {
        TemporaryCredentialsEmailDeliveryService emailDeliveryService =
                mock(TemporaryCredentialsEmailDeliveryService.class);
        RegistrationEmailDispatcher dispatcher = new RegistrationEmailDispatcher(emailDeliveryService);

        dispatcher.sendAfterCommit("auth-id", "user@example.com", "User", "Secret123!");

        verify(emailDeliveryService).send("user@example.com", "User", "Secret123!");
    }

    @Test
    void sendAfterCommit_whenTransactionIsActive_defersCredentialEmailUntilCommit() {
        TemporaryCredentialsEmailDeliveryService emailDeliveryService =
                mock(TemporaryCredentialsEmailDeliveryService.class);
        RegistrationEmailDispatcher dispatcher = new RegistrationEmailDispatcher(emailDeliveryService);
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.sendAfterCommit("auth-id", "user@example.com", "User", "Secret123!");

        verifyNoInteractions(emailDeliveryService);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);

        verify(emailDeliveryService).send("user@example.com", "User", "Secret123!");
    }

    @Test
    void sendAfterCommit_whenEmailFails_exposesOnlySafeFailure() {
        TemporaryCredentialsEmailDeliveryService emailDeliveryService =
                mock(TemporaryCredentialsEmailDeliveryService.class);
        doThrow(new RuntimeException("provider response includes Secret123!"))
                .when(emailDeliveryService).send("user@example.com", "User", "Secret123!");
        RegistrationEmailDispatcher dispatcher = new RegistrationEmailDispatcher(emailDeliveryService);

        assertThatThrownBy(() -> dispatcher.sendAfterCommit(
                "auth-id", "user@example.com", "User", "Secret123!"))
                .isInstanceOf(RegistrationEmailDeliveryException.class)
                .hasMessage("Registration credentials email could not be delivered")
                .hasMessageNotContaining("Secret123!")
                .hasMessageNotContaining("provider response");
    }
}
