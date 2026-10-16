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
public class AdminPaymentFilter {

    private PaymentStatus status;

    private UUID studentId;

    private UUID courseId;

    private LocalDateTime createdFrom;

    private LocalDateTime createdTo;

    private String reference;
}
