package com.mentify.service.stripe;

public interface StripePaymentIntentGateway {

    StripePaymentIntent createPaymentIntent(CreateStripePaymentIntentRequest request);
}
