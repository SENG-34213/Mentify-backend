package com.mentify.service.registration;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class TemporaryCredentialsEmailDeliveryService {

    private final JavaMailSender mailSender;

    @Value("${mentify.registration.email.from:no-reply@mentify.local}")
    private String fromAddress = "no-reply@mentify.local";

    public void send(String email, String firstName, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject("Your temporary Mentify sign-in password");
            helper.setText(plainText(firstName, temporaryPassword), htmlText(firstName, temporaryPassword));
            mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            // Keep exception details out of logs because mail implementations can include message content.
            throw new TemporaryCredentialsEmailException();
        }
    }

    private String plainText(String firstName, String temporaryPassword) {
        return "Hello " + safeName(firstName) + ",\n\n"
                + "Your Mentify account has been created. Your temporary password is:\n\n"
                + temporaryPassword + "\n\n"
                + "Follow the separate account setup email to verify your email and choose a permanent "
                + "password before signing in to Mentify. Keycloak requires this password update. "
                + "Keep this password private and delete this email "
                + "after setting your new password.\n";
    }

    private String htmlText(String firstName, String temporaryPassword) {
        return """
                <!doctype html>
                <html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:24px;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;color:#1f2937">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px">
                    <tr><td style="padding:28px 32px 12px;font-size:13px;font-weight:bold;letter-spacing:1px;color:#4f46e5">MENTIFY</td></tr>
                    <tr><td style="padding:8px 32px 0;font-size:24px;line-height:1.3;font-weight:bold">Your account is ready</td></tr>
                    <tr><td style="padding:16px 32px 8px;font-size:16px;line-height:1.6">Hello %s, your temporary Mentify password is:</td></tr>
                    <tr><td style="padding:8px 32px 20px"><div style="padding:16px;background:#f7f8fc;border:1px solid #e8eaf2;border-radius:8px;font-family:monospace;font-size:18px;letter-spacing:1px;word-break:break-all">%s</div></td></tr>
                    <tr><td style="padding:0 32px 28px;font-size:14px;line-height:1.6;color:#4b5565">Follow the separate account setup email to verify your email and choose a permanent password before signing in to Mentify. Keycloak requires this password update. Keep this password private and delete this email after setting your new password.</td></tr>
                  </table>
                </body></html>
                """.formatted(HtmlUtils.htmlEscape(safeName(firstName)), HtmlUtils.htmlEscape(temporaryPassword));
    }

    private String safeName(String firstName) {
        return firstName == null || firstName.isBlank() ? "there" : firstName.trim();
    }
}
