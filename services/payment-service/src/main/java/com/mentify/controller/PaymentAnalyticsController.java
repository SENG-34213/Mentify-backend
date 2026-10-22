package com.mentify.controller;

import com.mentify.dto.PaymentAnalyticsOverviewResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.PaymentAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/analytics/payments")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class PaymentAnalyticsController {

    private final PaymentAnalyticsService paymentAnalyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<PaymentAnalyticsOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(200, "Payment analytics fetched successfully",
                paymentAnalyticsService.getOverview()));
    }
}
