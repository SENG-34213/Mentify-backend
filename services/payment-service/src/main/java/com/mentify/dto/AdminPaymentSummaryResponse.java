package com.mentify.dto;

import com.mentify.enums.PaymentStatus;
import com.mentify.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentSummaryResponse {

    private UUID paymentId;

    private UUID studentId;

    private String buyerName;

    private String buyerEmail;

    private String buyerPhone;

    private PaymentType paymentType;

    private UUID marketplaceProductId;

    private String productName;

    private UUID courseId;

    private BigDecimal amount;

    private String currency;

    private PaymentStatus status;

    private String stripePaymentIntentId;

    private LocalDateTime paidAt;

    private LocalDateTime refundedAt;

    private LocalDateTime createdAt;
}
