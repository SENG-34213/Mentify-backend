package com.mentify.dto;

import com.mentify.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartCoursePaymentResponse {

    private UUID paymentId;

    private UUID courseId;

    private BigDecimal amount;

    private String currency;

    private PaymentStatus status;

    private String clientSecret;
}
