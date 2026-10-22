package com.mentify.analytics.client;

import com.mentify.analytics.config.FeignAuthorizationConfig;
import com.mentify.analytics.dto.PaymentAnalyticsOverview;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "payment-service", contextId = "analyticsPaymentClient",
        configuration = FeignAuthorizationConfig.class)
public interface PaymentAnalyticsClient {

    @GetMapping("/api/v1/internal/analytics/payments/overview")
    ApiResponse<PaymentAnalyticsOverview> getOverview();
}
