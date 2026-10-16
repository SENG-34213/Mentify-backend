package com.mentify.service.registration;

import com.mentify.service.KeycloakUserService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PasswordSetupEmailDispatcherTest {

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
}
