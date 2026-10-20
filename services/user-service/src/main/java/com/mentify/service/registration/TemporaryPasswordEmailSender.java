package com.mentify.service.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemporaryPasswordEmailSender {

    private final JavaMailSender mailSender;

    @Value("${mentify.registration.email.from:no-reply@mentify.local}")
    private String fromAddress = "no-reply@mentify.local";

    public void send(String recipient, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("[Mentify] Your temporary password");
        message.setText("""
                Your Mentify account is ready.

                Sign in with your email address and this temporary password:
                %s

                If you did not expect this message, contact your Mentify administrator.
                """.formatted(password));
        mailSender.send(message);
    }
}
