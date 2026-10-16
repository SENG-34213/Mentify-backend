package com.mentify.service.registration;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailSendException;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemporaryCredentialsEmailDeliveryServiceTest {

    @Test
    void send_deliversPlainTextAndHtmlEmailContainingTemporaryCredential() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        TemporaryCredentialsEmailDeliveryService service = new TemporaryCredentialsEmailDeliveryService(mailSender);

        service.send("student@example.com", "Test User", "Secure!Temp123");

        verify(mailSender).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("Your temporary Mentify sign-in password");
        assertThat(mimeMessage.getAllRecipients()).isNotEmpty();
        String content = contentAsText(mimeMessage.getContent());
        assertThat(content).contains("Secure!Temp123");
        assertThat(content).contains("<!doctype html>");
        assertThat(content).contains("Your temporary password is");
        assertThat(content).contains("Keycloak requires this password update");
    }

    @Test
    void send_whenSmtpFails_doesNotExposeCredentialInFailure() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(
                new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("SMTP rejected message containing Secure!Temp123"))
                .when(mailSender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
        TemporaryCredentialsEmailDeliveryService service = new TemporaryCredentialsEmailDeliveryService(mailSender);

        assertThatThrownBy(() -> service.send("student@example.com", "Test User", "Secure!Temp123"))
                .isInstanceOf(TemporaryCredentialsEmailException.class)
                .hasMessage("Temporary credentials email could not be delivered")
                .hasMessageNotContaining("Secure!Temp123");
    }

    private String contentAsText(Object content) throws Exception {
        if (content instanceof jakarta.mail.Multipart multipart) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                text.append(contentAsText(multipart.getBodyPart(i).getContent())).append('\n');
            }
            return text.toString();
        }
        return content == null ? "" : content.toString();
    }
}
