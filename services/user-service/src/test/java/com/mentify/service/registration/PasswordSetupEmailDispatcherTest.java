package com.mentify.service.registration;

import com.mentify.service.KeycloakUserService;
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

class PasswordSetupEmailDispatcherTest {

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sendAfterCommit_whenEitherDeliveryFails_reportsSafeFailureAndAttemptsBothMessages() {
        KeycloakUserService keycloakUserService = mock(KeycloakUserService.class);
        TemporaryCredentialsEmailDeliveryService emailDeliveryService = mock(TemporaryCredentialsEmailDeliveryService.class);
        doThrow(new RuntimeException("provider details may contain private message content"))
                .when(keycloakUserService).sendPasswordSetupEmail("keycloak-id");
        PasswordSetupEmailDispatcher dispatcher = new PasswordSetupEmailDispatcher(
                keycloakUserService, emailDeliveryService);

        assertThatThrownBy(() -> dispatcher.sendAfterCommit(
                "keycloak-id", "user@example.com", "User", "Secret123!"))
                .isInstanceOf(RegistrationEmailDeliveryException.class)
                .hasMessage("Account setup email could not be delivered")
                .hasMessageNotContaining("Secret123!")
                .hasMessageNotContaining("provider details");

        verify(keycloakUserService).sendPasswordSetupEmail("keycloak-id");
        verify(emailDeliveryService).send("user@example.com", "User", "Secret123!");
    }

    @Test
    void sendAfterCommit_whenDeliveryFailsAfterCommit_propagatesSafeFailureToRegistrationCaller() {
        KeycloakUserService keycloakUserService = mock(KeycloakUserService.class);
        TemporaryCredentialsEmailDeliveryService emailDeliveryService = mock(TemporaryCredentialsEmailDeliveryService.class);
        doThrow(new TemporaryCredentialsEmailException())
                .when(emailDeliveryService).send("user@example.com", "User", "Secret123!");
        PasswordSetupEmailDispatcher dispatcher = new PasswordSetupEmailDispatcher(
                keycloakUserService, emailDeliveryService);
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.sendAfterCommit("keycloak-id", "user@example.com", "User", "Secret123!");

        verifyNoInteractions(keycloakUserService, emailDeliveryService);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThatThrownBy(() -> synchronizations.forEach(TransactionSynchronization::afterCommit))
                .isInstanceOf(RegistrationEmailDeliveryException.class)
                .hasMessage("Account setup email could not be delivered")
                .hasMessageNotContaining("Secret123!");

        verify(keycloakUserService).sendPasswordSetupEmail("keycloak-id");
        verify(emailDeliveryService).send("user@example.com", "User", "Secret123!");
    }
}
