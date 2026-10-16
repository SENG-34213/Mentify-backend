package com.mentify.client;

import com.mentify.client.dto.PaymentVerificationResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "payment-service", path = "/api/v1/payments")
public interface PaymentServiceClient {

    @GetMapping("/internal/students/{studentId}/courses/{courseId}/successful")
    ApiResponse<PaymentVerificationResponse> verifySuccessfulPayment(
            @PathVariable UUID studentId,
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
