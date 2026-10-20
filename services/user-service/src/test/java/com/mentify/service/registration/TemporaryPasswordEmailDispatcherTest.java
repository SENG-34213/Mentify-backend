package com.mentify.service.registration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
class TemporaryPasswordEmailDispatcherTest {

    @Test
    void sendAfterCommit_doesNotLogPasswordWhenEmailDeliveryFails(CapturedOutput output) {
        TemporaryPasswordEmailSender emailSender = mock(TemporaryPasswordEmailSender.class);
        String password = "A7!bcD2x";
        doThrow(new MailSendException(password))
                .when(emailSender)
                .send("student@example.com", password);
        TemporaryPasswordEmailDispatcher dispatcher = new TemporaryPasswordEmailDispatcher(emailSender);

        dispatcher.sendAfterCommit("keycloak-user-id", "student@example.com", password);

        assertThat(output.getAll()).doesNotContain(password);
    }

    @Test
    void send_whenEmailDeliveryFails_throwsControlledErrorWithoutPassword() {
        TemporaryPasswordEmailSender emailSender = mock(TemporaryPasswordEmailSender.class);
        String password = "A7!bcD2x";
        doThrow(new MailSendException(password))
                .when(emailSender)
                .send("student@example.com", password);
        TemporaryPasswordEmailDispatcher dispatcher = new TemporaryPasswordEmailDispatcher(emailSender);

        assertThatThrownBy(() -> dispatcher.send("student@example.com", password))
                .isInstanceOf(TemporaryPasswordEmailException.class)
                .hasMessage("Temporary password email service is unavailable")
                .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain(password));
    }
}
