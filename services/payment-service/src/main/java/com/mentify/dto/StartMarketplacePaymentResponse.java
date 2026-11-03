package com.mentify.dto;

import com.mentify.enums.PaymentStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class StartMarketplacePaymentResponse {
    UUID paymentId;
    UUID productId;
    String productName;
    BigDecimal amount;
    String currency;
    PaymentStatus status;
    String clientSecret;
}
