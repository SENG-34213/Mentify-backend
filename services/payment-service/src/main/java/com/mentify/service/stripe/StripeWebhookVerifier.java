package com.mentify.service.stripe;

public interface StripeWebhookVerifier {

    StripeWebhookEvent verify(String payload, String signatureHeader);
}
