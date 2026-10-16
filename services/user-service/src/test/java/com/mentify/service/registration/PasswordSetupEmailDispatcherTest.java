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
    void sendAfterCommit_whenDeliveryFails_reportsSafeFailure() {
        KeycloakUserService keycloakUserService = mock(KeycloakUserService.class);
        doThrow(new RuntimeException("provider details may contain private message content"))
                .when(keycloakUserService).sendPasswordSetupEmail("keycloak-id");
        PasswordSetupEmailDispatcher dispatcher = new PasswordSetupEmailDispatcher(keycloakUserService);

        assertThatThrownBy(() -> dispatcher.sendAfterCommit("keycloak-id"))
                .isInstanceOf(RegistrationEmailDeliveryException.class);

        verify(keycloakUserService).sendPasswordSetupEmail("keycloak-id");
    }

    @Test
    void sendAfterCommit_whenDeliveryFailsAfterCommit_propagatesSafeFailureToRegistrationCaller() {
        KeycloakUserService keycloakUserService = mock(KeycloakUserService.class);
        doThrow(new RuntimeException("fail"))
                .when(keycloakUserService).sendPasswordSetupEmail("keycloak-id");
        PasswordSetupEmailDispatcher dispatcher = new PasswordSetupEmailDispatcher(keycloakUserService);
        TransactionSynchronizationManager.initSynchronization();

        dispatcher.sendAfterCommit("keycloak-id");

        verifyNoInteractions(keycloakUserService);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertThatThrownBy(() -> synchronizations.forEach(TransactionSynchronization::afterCommit))
                .isInstanceOf(RegistrationEmailDeliveryException.class);

        verify(keycloakUserService).sendPasswordSetupEmail("keycloak-id");
    }
}
