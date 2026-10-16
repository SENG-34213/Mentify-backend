package com.mentify.service.stripe;

public interface StripeRefundGateway {

    StripeRefund createRefund(CreateStripeRefundRequest request);
}
