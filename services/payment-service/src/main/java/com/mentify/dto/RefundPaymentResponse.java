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

    private String courseName;

    private String courseCode;

    private java.math.BigDecimal amount;

    private String currency;

    private String refundReason;

    private String stripeRefundId;

    private LocalDateTime refundedAt;
}
