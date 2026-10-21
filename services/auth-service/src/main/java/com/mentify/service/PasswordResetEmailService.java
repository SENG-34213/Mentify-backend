package com.mentify.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {
    private final JavaMailSender mailSender;

    @Value("${mentify.password-reset.email.from:no-reply@mentify.local}")
    private String from;

    public void send(String email, String otp, int expiryMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("Your Mentify password reset code");
            helper.setText("Your Mentify password reset code is " + otp + ". It expires in "
                    + expiryMinutes + " minutes. If you did not request this, you can ignore this email.", false);
            mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            throw new IllegalStateException("Password reset email could not be delivered");
        }
    }
}
