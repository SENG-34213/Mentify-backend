package com.mentify.dto;

import com.mentify.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPaymentResponse {

    private UUID paymentId;

    private PaymentStatus status;

    private String stripeRefundId;

    private LocalDateTime refundedAt;
}
