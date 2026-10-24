package com.mentify.service.registration;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        assertThat(mimeMessage.getSubject()).isEqualTo("Your TiT account is ready — secure sign-in details");
        assertThat(mimeMessage.getAllRecipients()).isNotEmpty();
        assertThat(mimeMessage.getContent()).isInstanceOf(Multipart.class);
        List<String> leafMimeTypes = new ArrayList<>();
        collectLeafMimeTypes(mimeMessage, leafMimeTypes);
        assertThat(leafMimeTypes).contains("text/plain", "text/html");
        String content = contentAsText(mimeMessage.getContent());
        assertThat(content).contains("Secure!Temp123");
        assertThat(content).contains("<!doctype html>");
        assertThat(content).contains("student@example.com");
        assertThat(content).contains("https://app.mentify.example/login");
        assertThat(content).contains("Temporary password:");
        assertThat(content).contains("Your TiT workspace is ready");
        assertThat(content).contains("Important security notice:");
        assertThat(content).contains("Change it immediately after your first sign-in");
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
        verify(mailSender, times(2)).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

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

    private void collectLeafMimeTypes(Part part, List<String> mimeTypes) throws Exception {
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                collectLeafMimeTypes(multipart.getBodyPart(index), mimeTypes);
            }
            return;
        }

        mimeTypes.add(part.getContentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT));
    }

}
