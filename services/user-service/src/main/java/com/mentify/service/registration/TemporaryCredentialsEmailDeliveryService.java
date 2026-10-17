package com.mentify.service.registration;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class TemporaryCredentialsEmailDeliveryService {

    private final JavaMailSender mailSender;

    @Value("${mentify.registration.email.from:no-reply@mentify.local}")
    private String fromAddress = "no-reply@mentify.local";

    @Value("${mentify.registration.email.login-url:http://localhost:5173/login}")
    private String loginUrl = "http://localhost:5173/login";

    @Value("${mentify.registration.email.retry-attempts:2}")
    private int retryAttempts = 2;

    public void send(String email, String firstName, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject("Your temporary Mentify sign-in password");
            helper.setText(plainText(email, firstName, temporaryPassword), htmlText(email, firstName, temporaryPassword));
            int attempts = Math.max(1, retryAttempts);
            for (int attempt = 1; ; attempt++) {
                try {
                    mailSender.send(message);
                    return;
                } catch (MailException exception) {
                    if (attempt >= attempts) {
                        throw exception;
                    }
                }
            }
        } catch (MailException | MessagingException exception) {
            // Keep exception details out of logs because mail implementations can include message content.
//            throw new TemporaryCredentialsEmailException();
        }
    }

    private String plainText(String email, String firstName, String temporaryPassword) {
        return "Hello " + safeName(firstName) + ",\n\n"
                + "Your Mentify account has been created. Sign in with " + email + " and this temporary password:\n\n"
                + temporaryPassword + "\n\n"
                + "Sign in at " + loginUrl + " with these credentials. You can change the password later using "
                + "Forgot password on the sign-in page. Keep this message private and delete it after changing "
                + "your password.\n";
    }

    private String htmlText(String email, String firstName, String temporaryPassword) {
        String safeLoginUrl = StringUtils.hasText(loginUrl) ? loginUrl.trim() : "http://localhost:5173/login";
        return """
                <!doctype html>
                <html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:24px;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;color:#1f2937">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:600px;margin:0 auto;background:#fff;border-radius:12px">
                    <tr><td style="padding:28px 32px 12px;font-size:13px;font-weight:bold;letter-spacing:1px;color:#4f46e5">MENTIFY</td></tr>
                    <tr><td style="padding:8px 32px 0;font-size:24px;line-height:1.3;font-weight:bold">Your account is ready</td></tr>
                    <tr><td style="padding:16px 32px 8px;font-size:16px;line-height:1.6">Hello %s, your Mentify account is ready.</td></tr>
                    <tr><td style="padding:8px 32px;font-size:14px;line-height:1.6">Sign-in email: <strong>%s</strong></td></tr>
                    <tr><td style="padding:8px 32px 0;font-size:14px;line-height:1.6">Temporary password:</td></tr>
                    <tr><td style="padding:8px 32px 20px"><div style="padding:16px;background:#f7f8fc;border:1px solid #e8eaf2;border-radius:8px;font-family:monospace;font-size:18px;letter-spacing:1px;word-break:break-all">%s</div></td></tr>
                    <tr><td style="padding:0 32px 16px;font-size:14px;line-height:1.6;color:#4b5565">Sign in to Mentify with these credentials. You can change your password later using Forgot password on the sign-in page.</td></tr>
                    <tr><td style="padding:0 32px 24px"><a href="%s" style="display:inline-block;padding:12px 20px;background:#4f46e5;color:#fff;text-decoration:none;border-radius:8px;font-weight:bold">Open Mentify</a></td></tr>
                    <tr><td style="padding:0 32px 28px;font-size:13px;line-height:1.6;color:#6b7280">Keep this password private and delete this email after changing your password.</td></tr>
                  </table>
                </body></html>
                """.formatted(HtmlUtils.htmlEscape(safeName(firstName)), HtmlUtils.htmlEscape(email),
                HtmlUtils.htmlEscape(temporaryPassword), HtmlUtils.htmlEscape(safeLoginUrl));
    }

    private String safeName(String firstName) {
        return firstName == null || firstName.isBlank() ? "there" : firstName.trim();
    }
}
