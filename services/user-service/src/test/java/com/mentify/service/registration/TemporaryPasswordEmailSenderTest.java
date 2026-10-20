package com.mentify.service.registration;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TemporaryPasswordEmailSenderTest {

    @Test
    void send_includesTheGeneratedPasswordInTheRegistrationEmail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        TemporaryPasswordEmailSender sender = new TemporaryPasswordEmailSender(mailSender);
        String password = "A7!bcD2x";

        sender.send("student@example.com", password);

        var messageCaptor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@mentify.local");
        assertThat(message.getTo()).containsExactly("student@example.com");
        assertThat(message.getText()).contains(password);
    }
}
