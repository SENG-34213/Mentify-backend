package com.mentify.service.stripe;

import com.mentify.config.StripeProperties;
import com.mentify.exception.PaymentDomainException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class StripeWebhookVerifierImpl implements StripeWebhookVerifier {

    private final StripeProperties stripeProperties;

    @Override
    public StripeWebhookEvent verify(String payload, String signatureHeader) {
        if (stripeProperties.getWebhookSecret() == null || stripeProperties.getWebhookSecret().isBlank()) {
            throw new PaymentDomainException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe webhook secret is not configured");
        }

        try {
            Event event = Webhook.constructEvent(payload, signatureHeader, stripeProperties.getWebhookSecret());
            PaymentIntent paymentIntent = extractPaymentIntent(event);
            return new StripeWebhookEvent(
                    event.getId(),
                    event.getType(),
                    paymentIntent.getId(),
                    toLocalDateTime(event.getCreated())
            );
        } catch (SignatureVerificationException ex) {
            throw new PaymentDomainException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature");
        } catch (EventDataObjectDeserializationException | IllegalArgumentException ex) {
            throw new PaymentDomainException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook payload");
        }
    }

    private PaymentIntent extractPaymentIntent(Event event) throws EventDataObjectDeserializationException {
        StripeObject stripeObject = event.getDataObjectDeserializer().deserializeUnsafe();

        if (!(stripeObject instanceof PaymentIntent paymentIntent) || paymentIntent.getId() == null) {
            throw new IllegalArgumentException("Webhook event does not contain a PaymentIntent");
        }
        return paymentIntent;
    }

    private LocalDateTime toLocalDateTime(Long created) {
        Instant instant = created == null ? Instant.now() : Instant.ofEpochSecond(created);
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
