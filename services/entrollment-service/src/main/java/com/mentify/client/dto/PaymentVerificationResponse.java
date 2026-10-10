package com.mentify.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentVerificationResponse {

    private UUID studentId;

    private UUID courseId;

    private boolean successfulPaymentExists;
}
