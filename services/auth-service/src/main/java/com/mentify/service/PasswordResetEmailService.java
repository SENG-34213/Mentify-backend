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
            // Multipart mode is required when sending both plain-text and HTML alternatives.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("Your TiT password reset code");
            helper.setText(plainText(otp, expiryMinutes), htmlContent(otp, expiryMinutes));
            mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            throw new IllegalStateException("Password reset email could not be delivered");
        }
    }

    private String plainText(String otp, int expiryMinutes) {
        return "Hello,\n\n"
                + "We received a request to reset your TiT password.\n\n"
                + "Your verification code: " + otp + "\n\n"
                + "This code expires in " + expiryMinutes + " minutes and can be used only once.\n\n"
                + "If you did not request a password reset, you can safely ignore this email. "
                + "Your account remains secure.\n\n"
                + "Regards,\nThe TiT team";
    }

    private String htmlContent(String otp, int expiryMinutes) {
        return """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Your TiT password reset code</title>
                  </head>
                  <body style="margin:0;background:#f1f5f9;color:#0f172a;font-family:Arial,Helvetica,sans-serif;">
                    <div style="display:none;max-height:0;overflow:hidden;opacity:0;">
                      Your TiT verification code is ready. It expires in %d minutes.
                    </div>
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f1f5f9;padding:32px 12px;">
                      <tr>
                        <td align="center">
                          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:580px;background:#ffffff;border:1px solid #e2e8f0;border-radius:20px;overflow:hidden;">
                            <tr>
                              <td style="background:linear-gradient(135deg,#4338ca,#7c3aed);padding:30px 36px;color:#ffffff;">
                                <div style="font-size:26px;font-weight:800;letter-spacing:-0.5px;">TiT</div>
                                <div style="margin-top:7px;font-size:13px;color:#e0e7ff;">Learning workspace</div>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:36px;">
                                <div style="font-size:13px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;color:#4f46e5;">Password reset</div>
                                <h1 style="margin:12px 0 0;font-size:28px;line-height:1.25;color:#0f172a;">Reset your password</h1>
                                <p style="margin:16px 0 0;font-size:15px;line-height:1.7;color:#475569;">
                                  We received a request to reset the password for your TiT account. Enter the verification code below to continue.
                                </p>
                                <div style="margin:28px 0;padding:24px 18px;text-align:center;background:#eef2ff;border:1px solid #c7d2fe;border-radius:16px;">
                                  <div style="font-size:11px;font-weight:700;letter-spacing:1.8px;text-transform:uppercase;color:#6366f1;">Your verification code</div>
                                  <div style="margin-top:16px;">%s</div>
                                  <div style="margin-top:15px;font-size:12px;color:#6366f1;">Enter each digit exactly as shown</div>
                                </div>
                                <p style="margin:0;font-size:13px;line-height:1.6;color:#64748b;">
                                  <span style="display:inline-block;padding:5px 10px;background:#fef3c7;border-radius:999px;color:#92400e;font-weight:700;">Expires in %d minutes</span>
                                  <span style="margin-left:6px;">This code can be used only once.</span>
                                </p>
                                <div style="margin-top:26px;padding:15px 16px;background:#f8fafc;border-left:4px solid #6366f1;border-radius:8px;">
                                  <p style="margin:0;font-size:13px;line-height:1.6;color:#475569;">
                                    <strong style="color:#1e293b;">Didn't request this?</strong><br>
                                    You can safely ignore this email. No changes will be made to your account.
                                  </p>
                                </div>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:22px 36px;background:#f8fafc;border-top:1px solid #e2e8f0;">
                                <p style="margin:0;font-size:12px;line-height:1.6;color:#64748b;">
                                  For your security, never share this code with anyone. TiT will never ask for your verification code by phone or message.
                                </p>
                                <p style="margin:12px 0 0;font-size:12px;color:#94a3b8;">© TiT · Secure learning for every journey</p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(expiryMinutes, verificationCodeMarkup(otp), expiryMinutes);
    }

    private String verificationCodeMarkup(String otp) {
        StringBuilder cells = new StringBuilder("<table role=\"presentation\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>");
        for (int index = 0; index < otp.length(); index++) {
            if (index > 0) {
                cells.append("<td style=\"width:7px;\"></td>");
            }
            cells.append("<td style=\"width:42px;height:50px;background:#ffffff;border:1px solid #a5b4fc;border-radius:10px;vertical-align:middle;font-size:28px;line-height:50px;font-weight:800;color:#312e81;\">")
                    .append(otp.charAt(index))
                    .append("</td>");
        }
        return cells.append("</tr></table>").toString();
    }
}
