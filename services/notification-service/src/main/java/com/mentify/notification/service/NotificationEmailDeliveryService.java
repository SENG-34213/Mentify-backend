package com.mentify.notification.service;

import com.mentify.notification.entity.EmailDeliveryStatus;
import com.mentify.notification.entity.Notification;
import com.mentify.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailDeliveryService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${mentify.notifications.email.from:no-reply@mentify.local}")
    private String fromAddress;

    @Value("${mentify.notifications.email.retry-delay-millis:30000}")
    private long retryDelayMillis;

    @Value("${mentify.notifications.email.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public void deliver(UUID notificationId) {
        Notification notification = notificationRepository.findByIdForEmailDelivery(notificationId).orElse(null);
        if (notification == null || notification.getEmailStatus() != EmailDeliveryStatus.PENDING) {
            return;
        }
        if (!NotificationEmailPolicy.allowsEmail(notification.getType())) {
            notification.setEmailStatus(EmailDeliveryStatus.NOT_REQUESTED);
            notification.setRecipientEmail(null);
            notification.setNextEmailAttemptAt(null);
            return;
        }

        try {
            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(notification.getRecipientEmail());
            helper.setSubject("[Mentify] " + notification.getTitle());
            helper.setText(plainText(notification), htmlText(notification));
            mailSender.send(mail);
            notification.setEmailStatus(EmailDeliveryStatus.SENT);
            notification.setEmailAttempts(notification.getEmailAttempts() + 1);
            notification.setRecipientEmail(null);
            notification.setLastEmailError(null);
            notification.setNextEmailAttemptAt(null);
        } catch (MailException exception) {
            recordFailure(notification, notificationId, exception);
        } catch (MessagingException exception) {
            recordFailure(notification, notificationId,
                    new MailPreparationException("Could not prepare notification email", exception));
        }
    }

    private String plainText(Notification notification) {
        return notification.getTitle() + "\n\n" + notification.getMessage()
                + "\n\nYou received this email because email notifications are enabled for your Mentify account.";
    }

    private String htmlText(Notification notification) {
        if ("PAYMENT_SUCCEEDED".equalsIgnoreCase(notification.getType())) {
            return paymentReceiptHtml(notification);
        }

        String safeTitle = HtmlUtils.htmlEscape(notification.getTitle());
        String safeMessage = HtmlUtils.htmlEscape(notification.getMessage())
                .replace("\r\n", "\n").replace('\r', '\n').replace("\n", "<br>");
        return """
                <!doctype html>
                <html lang="en">
                  <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>%s</title></head>
                  <body style="margin:0;padding:24px;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:12px;">
                      <tr><td style="padding:28px 32px 12px;font-size:13px;font-weight:bold;letter-spacing:1px;color:#4f46e5;">MENTIFY</td></tr>
                      <tr><td style="padding:8px 32px 0;font-size:24px;line-height:1.3;font-weight:bold;">%s</td></tr>
                      <tr><td style="padding:16px 32px 32px;font-size:16px;line-height:1.6;">%s</td></tr>
                    </table>
                    <p style="max-width:600px;margin:16px auto 0;text-align:center;font-size:12px;line-height:1.5;color:#6b7280;">You received this email because email notifications are enabled for your Mentify account.</p>
                  </body>
                </html>
                """.formatted(safeTitle, safeTitle, safeMessage);
    }

    private String paymentReceiptHtml(Notification notification) {
        String message = notification.getMessage() == null ? "" : notification.getMessage();
        String product = between(message, "Your payment for ", " was completed successfully.");
        String amount = between(message, "Amount: ", "; paid at:");
        String paidAt = between(message, "; paid at: ", "; payment ID:");
        String customerName = between(message, "; customer name: ", "; customer email:");
        String customerEmail = between(message, "; customer email: ", "; payment ID:");
        String paymentId = between(message, "; payment ID: ", "; Stripe reference:");
        String stripeReference = between(message, "; Stripe reference: ", ". Thank you for your purchase.");

        if (product.isBlank()) product = "TiT Online Education purchase";
        if (amount.isBlank()) amount = "Payment confirmed";
        paidAt = formatPaymentDate(paidAt);
        if (customerName.isBlank()) customerName = "—";
        if (customerEmail.isBlank()) customerEmail = notification.getRecipientEmail() == null ? "—" : notification.getRecipientEmail();
        if (paymentId.isBlank()) paymentId = "—";
        if (stripeReference.isBlank()) stripeReference = "—";

        return """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Payment receipt</title>
                  </head>
                  <body style="margin:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#172033;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#f4f7fb;padding:28px 12px;">
                      <tr><td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:620px;background:#ffffff;border:1px solid #e5eaf2;border-radius:18px;overflow:hidden;">
                          <tr><td style="padding:24px 30px;background:#172554;color:#ffffff;">
                            <div style="font-size:12px;font-weight:bold;letter-spacing:2px;color:#bfdbfe;">TiT ONLINE EDUCATION</div>
                            <div style="margin-top:10px;font-size:26px;line-height:1.25;font-weight:700;">Payment receipt</div>
                            <div style="margin-top:6px;font-size:14px;color:#dbeafe;">Thank you for your purchase.</div>
                          </td></tr>
                          <tr><td style="padding:28px 30px 12px;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                              <tr>
                                <td style="font-size:15px;line-height:1.6;color:#526078;">Your payment was completed successfully.</td>
                                <td align="right" style="white-space:nowrap;"><span style="display:inline-block;padding:7px 11px;border-radius:999px;background:#dcfce7;color:#166534;font-size:12px;font-weight:bold;">PAID</span></td>
                              </tr>
                            </table>
                            <div style="margin-top:22px;padding:20px;border:1px solid #dbe4f0;border-radius:14px;background:#f8fbff;">
                              <div style="font-size:12px;font-weight:bold;letter-spacing:.7px;text-transform:uppercase;color:#71809a;">Purchase</div>
                              <div style="margin-top:8px;font-size:18px;line-height:1.4;font-weight:700;color:#172033;">%s</div>
                              <div style="margin-top:18px;font-size:12px;font-weight:bold;letter-spacing:.7px;text-transform:uppercase;color:#71809a;">Total paid</div>
                              <div style="margin-top:4px;font-size:28px;line-height:1.2;font-weight:800;color:#4338ca;">%s</div>
                            </div>
                          </td></tr>
                          <tr><td style="padding:12px 30px 26px;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;">
                              <tr><td style="padding:11px 0;border-bottom:1px solid #edf1f6;font-size:13px;color:#71809a;">Payment date</td><td align="right" style="padding:11px 0;border-bottom:1px solid #edf1f6;font-size:13px;font-weight:600;color:#172033;">%s</td></tr>
                              <tr><td style="padding:11px 0;border-bottom:1px solid #edf1f6;font-size:13px;color:#71809a;">Customer name</td><td align="right" style="padding:11px 0;border-bottom:1px solid #edf1f6;font-size:13px;font-weight:600;color:#172033;">%s</td></tr>
                              <tr><td style="padding:11px 0;border-bottom:1px solid #edf1f6;font-size:13px;color:#71809a;">Customer email</td><td align="right" style="padding:11px 0;border-bottom:1px solid #edf1f6;font-size:13px;font-weight:600;color:#172033;word-break:break-word;">%s</td></tr>
                              <tr><td style="padding:11px 0;border-bottom:1px solid #edf1f6;font-size:13px;color:#71809a;">Payment ID</td><td align="right" style="padding:11px 0;border-bottom:1px solid #edf1f6;font-size:12px;font-weight:600;color:#172033;word-break:break-all;">%s</td></tr>
                              <tr><td style="padding:11px 0;font-size:13px;color:#71809a;">Stripe reference</td><td align="right" style="padding:11px 0;font-size:12px;font-weight:600;color:#172033;word-break:break-all;">%s</td></tr>
                            </table>
                          </td></tr>
                          <tr><td style="padding:20px 30px;background:#f8fafc;border-top:1px solid #edf1f6;font-size:13px;line-height:1.65;color:#667085;">
                            Keep this email for your records. If you have any questions about this payment, please contact the TiT Online Education support team and include your payment ID.
                          </td></tr>
                        </table>
                        <div style="max-width:620px;padding:18px 12px 0;font-size:11px;line-height:1.6;color:#98a2b3;">This is an automated payment confirmation from TiT Online Education. Please do not reply to this email.</div>
                      </td></tr>
                    </table>
                  </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(product),
                HtmlUtils.htmlEscape(amount),
                HtmlUtils.htmlEscape(paidAt),
                HtmlUtils.htmlEscape(customerName),
                HtmlUtils.htmlEscape(customerEmail),
                HtmlUtils.htmlEscape(paymentId),
                HtmlUtils.htmlEscape(stripeReference)
        );
    }

    private String formatPaymentDate(String value) {
        if (value == null || value.isBlank()) return "—";
        try {
            return LocalDateTime.parse(value.trim())
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, h:mm a"));
        } catch (DateTimeParseException exception) {
            return value;
        }
    }

    private String between(String value, String start, String end) {
        int startIndex = value.indexOf(start);
        if (startIndex < 0) return "";
        startIndex += start.length();
        int endIndex = value.indexOf(end, startIndex);
        return (endIndex < 0 ? value.substring(startIndex) : value.substring(startIndex, endIndex)).trim();
    }

    private void recordFailure(Notification notification, UUID notificationId, MailException exception) {
        int attempts = notification.getEmailAttempts() + 1;
        notification.setEmailAttempts(attempts);
        notification.setLastEmailError(errorSummary(exception));
        if (attempts >= maxAttempts) {
            notification.setEmailStatus(EmailDeliveryStatus.FAILED);
            notification.setRecipientEmail(null);
            notification.setNextEmailAttemptAt(null);
        } else {
            notification.setNextEmailAttemptAt(Instant.now().plusMillis(retryDelayMillis));
        }
        log.error("Email delivery failed for notification {} (attempt {}/{})",
                notificationId, attempts, maxAttempts, exception);
    }

    private String errorSummary(MailException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 1000));
    }
}
