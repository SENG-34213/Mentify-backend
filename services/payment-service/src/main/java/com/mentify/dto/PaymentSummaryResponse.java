package com.mentify.dto;

import com.mentify.enums.PaymentStatus;
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
public class PaymentSummaryResponse {

    private UUID paymentId;

    private UUID courseId;

    private BigDecimal amount;

    private String currency;

    private PaymentStatus status;

    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
}
