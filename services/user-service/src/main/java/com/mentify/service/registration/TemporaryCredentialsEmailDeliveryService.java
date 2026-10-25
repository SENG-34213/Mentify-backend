package com.mentify.service.registration;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import com.mentify.enums.Role;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class TemporaryCredentialsEmailDeliveryService {

    private final JavaMailSender mailSender;

    @Value("${mentify.registration.email.from:no-reply@mentify.local}")
    private String fromAddress = "no-reply@mentify.local";

    @Value("${mentify.registration.email.login-url:http://localhost:5173/login}")
    private String loginUrl = "http://localhost:5173/login";

    @Value("${mentify.registration.email.student-login-url:http://localhost:5173/login}")
    private String studentLoginUrl = "http://localhost:5173/login";

    @Value("${mentify.registration.email.teacher-login-url:http://localhost:5173/login}")
    private String teacherLoginUrl = "http://localhost:5173/login";

    @Value("${mentify.registration.email.admin-login-url:http://localhost:5173/login}")
    private String adminLoginUrl = "http://localhost:5173/login";

    @Value("${mentify.registration.email.retry-attempts:2}")
    private int retryAttempts = 2;

    public void send(String email, String firstName, String temporaryPassword) {
        send(email, firstName, temporaryPassword, null);
    }

    public void send(String email, String firstName, String temporaryPassword, Role role) {
        try {
            String resolvedLoginUrl = resolveLoginUrl(role);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject("Your TiT account is ready — secure sign-in details");
            MimeMultipart alternative = new MimeMultipart("alternative");

            MimeBodyPart plainTextPart = new MimeBodyPart();
            plainTextPart.setText(plainText(email, firstName, temporaryPassword, resolvedLoginUrl), StandardCharsets.UTF_8.name());
            alternative.addBodyPart(plainTextPart);

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlText(email, firstName, temporaryPassword, resolvedLoginUrl),
                    "text/html; charset=" + StandardCharsets.UTF_8.name());
            htmlPart.setHeader("Content-Type", "text/html; charset=" + StandardCharsets.UTF_8.name());
            alternative.addBodyPart(htmlPart);

            message.setContent(alternative);
            message.saveChanges();
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
            throw new TemporaryCredentialsEmailException();
        }
    }

    private String plainText(String email, String firstName, String temporaryPassword, String resolvedLoginUrl) {
        return "Hello " + safeName(firstName) + ",\n\n"
                + "Your TiT account has been created. Sign in with " + email + " and this temporary password:\n\n"
                + temporaryPassword + "\n\n"
                + "Sign in at " + resolvedLoginUrl + " with these credentials. You will be asked to change this password "
                + "after signing in. Keep this message private, never forward it, and delete it after changing "
                + "your password.\n";
    }

    private String htmlText(String email, String firstName, String temporaryPassword, String resolvedLoginUrl) {
        String safeLoginUrl = StringUtils.hasText(resolvedLoginUrl) ? resolvedLoginUrl.trim() : "http://localhost:5173/login";
        return """
                <!doctype html>
                <html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:32px 16px;background:#eef2ff;font-family:Arial,Helvetica,sans-serif;color:#172033">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:620px;background:#ffffff;border:1px solid #e4e7f2;border-radius:20px;overflow:hidden;box-shadow:0 12px 32px rgba(31,41,55,.08)">
                        <tr><td style="padding:26px 36px;background:#312e81;color:#ffffff;font-size:20px;font-weight:800;letter-spacing:.5px">TiT <span style="font-size:12px;font-weight:600;letter-spacing:1.5px;color:#c7d2fe">LEARNING PLATFORM</span></td></tr>
                        <tr><td style="padding:38px 36px 10px;font-size:12px;line-height:18px;font-weight:800;letter-spacing:1.5px;text-transform:uppercase;color:#4f46e5">Welcome to TiT</td></tr>
                        <tr><td style="padding:0 36px;font-size:30px;line-height:38px;font-weight:800;letter-spacing:-.7px;color:#172033">Your account is ready</td></tr>
                        <tr><td style="padding:14px 36px 24px;font-size:16px;line-height:26px;color:#566176">Hello %s,<br><br>Your TiT workspace is ready. Use the secure sign-in details below to access your account.</td></tr>
                        <tr><td style="padding:0 36px 8px;font-size:13px;line-height:20px;font-weight:700;color:#667085">SIGN-IN EMAIL</td></tr>
                        <tr><td style="padding:0 36px 24px;font-size:16px;line-height:24px;font-weight:700;color:#172033">%s</td></tr>
                        <tr><td style="padding:0 36px 8px;font-size:13px;line-height:20px;font-weight:700;color:#667085">Temporary password:</td></tr>
                        <tr><td style="padding:0 36px 26px"><div style="padding:18px 20px;background:#f5f3ff;border:1px solid #ddd6fe;border-radius:12px;font-family:Consolas,Monaco,monospace;font-size:22px;line-height:28px;font-weight:800;letter-spacing:2px;color:#312e81;word-break:break-all">%s</div></td></tr>
                        <tr><td style="padding:0 36px 26px"><a href="%s" style="display:inline-block;padding:14px 24px;background:#4f46e5;color:#ffffff;text-decoration:none;border-radius:10px;font-size:15px;line-height:20px;font-weight:800">Sign in to TiT</a></td></tr>
                        <tr><td style="padding:20px 36px;background:#fffbeb;border-top:1px solid #fde68a;border-bottom:1px solid #fde68a;font-size:14px;line-height:22px;color:#713f12"><strong>Important security notice:</strong> This is a temporary password. Change it immediately after your first sign-in. Never share it, forward this email, or save it in an unsecured location.</td></tr>
                        <tr><td style="padding:24px 36px 32px;font-size:13px;line-height:21px;color:#667085">If you did not expect this account, please contact your administrator. For your protection, delete this email after changing your password.</td></tr>
                      </table>
                      <p style="margin:18px 0 0;font-size:12px;line-height:18px;color:#7c8699">This is an automated message from TiT. Please do not reply.</p>
                    </td></tr>
                  </table>
                </body></html>
                """.formatted(HtmlUtils.htmlEscape(safeName(firstName)), HtmlUtils.htmlEscape(email),
                HtmlUtils.htmlEscape(temporaryPassword), HtmlUtils.htmlEscape(safeLoginUrl));
    }

    private String safeName(String firstName) {
        return firstName == null || firstName.isBlank() ? "there" : firstName.trim();
    }

    private String resolveLoginUrl(Role role) {
        if (role == Role.STUDENT) return fallback(studentLoginUrl);
        if (role == Role.TEACHER) return fallback(teacherLoginUrl);
        if (role == Role.ADMIN || role == Role.SUPER_ADMIN) return fallback(adminLoginUrl);
        return fallback(loginUrl);
    }

    private String fallback(String value) {
        return StringUtils.hasText(value) ? value.trim() : "http://localhost:5173/login";
    }
}
