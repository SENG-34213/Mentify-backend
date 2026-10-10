package com.mentify.service.stripe;

import com.mentify.config.StripeProperties;
import com.mentify.exception.PaymentDomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeWebhookVerifierImplTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret";
    private static final long EVENT_CREATED = 1791600000L;
    private static final String PAYLOAD = """
            {
              "id": "evt_test_001",
              "object": "event",
              "api_version": "2026-09-30.endive",
              "created": %d,
              "type": "payment_intent.succeeded",
              "data": {
                "object": {
                  "id": "pi_test_001",
                  "object": "payment_intent"
                }
              }
            }
            """.formatted(EVENT_CREATED);

    private StripeWebhookVerifierImpl verifier;

    @BeforeEach
    void setUp() {
        StripeProperties stripeProperties = new StripeProperties();
        stripeProperties.setWebhookSecret(WEBHOOK_SECRET);
        verifier = new StripeWebhookVerifierImpl(stripeProperties);
    }

    @Test
    void verify_whenSignatureIsValid_returnsStripeWebhookEvent() {
        StripeWebhookEvent event = verifier.verify(PAYLOAD, signatureHeader(PAYLOAD, WEBHOOK_SECRET));

        assertThat(event.eventId()).isEqualTo("evt_test_001");
        assertThat(event.type()).isEqualTo("payment_intent.succeeded");
        assertThat(event.paymentIntentId()).isEqualTo("pi_test_001");
        assertThat(event.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 10, 2, 40));
    }

    @Test
    void verify_whenSignatureIsInvalid_rejectsWebhook() {
        assertThatThrownBy(() -> verifier.verify(PAYLOAD, "t=1791600000,v1=bad"))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessage("Invalid Stripe webhook signature");
    }

    @Test
    void verify_whenWebhookSecretMissing_rejectsWebhook() {
        StripeProperties stripeProperties = new StripeProperties();
        StripeWebhookVerifierImpl missingSecretVerifier = new StripeWebhookVerifierImpl(stripeProperties);

        assertThatThrownBy(() -> missingSecretVerifier.verify(PAYLOAD, signatureHeader(PAYLOAD, WEBHOOK_SECRET)))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessage("Stripe webhook secret is not configured");
    }

    private String signatureHeader(String payload, String secret) {
        long timestamp = Instant.now().getEpochSecond();
        String signedPayload = timestamp + "." + payload;
        return "t=" + timestamp + ",v1=" + hmacSha256(secret, signedPayload);
    }

    private String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign webhook payload", ex);
        }
    }
}
