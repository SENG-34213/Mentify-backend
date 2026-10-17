package com.mentify.service.registration;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemporaryCredentialsEmailDeliveryServiceTest {

    @Test
    void send_deliversPlainTextAndHtmlEmailContainingTemporaryCredential() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        TemporaryCredentialsEmailDeliveryService service = new TemporaryCredentialsEmailDeliveryService(mailSender);
        ReflectionTestUtils.setField(service, "loginUrl", "https://app.mentify.example/login");

        service.send("student@example.com", "Test User", "Secure!Temp123");

        verify(mailSender).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("Your temporary Mentify sign-in password");
        assertThat(mimeMessage.getAllRecipients()).isNotEmpty();
        assertThat(mimeMessage.isMimeType("multipart/mixed")).isTrue();
        jakarta.mail.Multipart messageParts = (jakarta.mail.Multipart) mimeMessage.getContent();
        assertThat(messageParts.getBodyPart(0).isMimeType("multipart/alternative")).isTrue();
        String content = contentAsText(mimeMessage.getContent());
        assertThat(content).contains("Secure!Temp123");
        assertThat(content).contains("<!doctype html>");
        assertThat(content).contains("student@example.com");
        assertThat(content).contains("https://app.mentify.example/login");
        assertThat(content).contains("Temporary password:");
        assertThat(content).contains("Sign in to Mentify with these credentials");
        assertThat(content).contains("Forgot password on the sign-in page");
    }

//    @Test
//    void send_whenSmtpFails_doesNotExposeCredentialInFailure() {
//        JavaMailSender mailSender = mock(JavaMailSender.class);
//        when(mailSender.createMimeMessage()).thenReturn(
//                new MimeMessage(Session.getInstance(new Properties())));
//        doThrow(new MailSendException("SMTP rejected message containing Secure!Temp123"))
//                .when(mailSender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
//        TemporaryCredentialsEmailDeliveryService service = new TemporaryCredentialsEmailDeliveryService(mailSender);
//
//        assertThatThrownBy(() -> service.send("student@example.com", "Test User", "Secure!Temp123"))
//                .isInstanceOf(TemporaryCredentialsEmailException.class)
//                .hasMessage("Temporary credentials email could not be delivered")
//                .hasMessageNotContaining("Secure!Temp123");
//        verify(mailSender, times(2)).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
//    }

    @Test
    void send_retriesTransientSmtpFailureWithoutPersistingCredential() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("temporary SMTP failure"))
                .doNothing()
                .when(mailSender).send(mimeMessage);
        TemporaryCredentialsEmailDeliveryService service = new TemporaryCredentialsEmailDeliveryService(mailSender);

        service.send("student@example.com", "Test User", "Secure!Temp123");

        verify(mailSender, times(2)).send(mimeMessage);
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
