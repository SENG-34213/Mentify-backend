package com.mentify.service.stripe;

public record StripeRefund(
        String id,
        String status
) {
}
