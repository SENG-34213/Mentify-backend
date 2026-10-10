package com.mentify.controller;

import com.mentify.dto.StartCoursePaymentRequest;
import com.mentify.dto.StartCoursePaymentResponse;
import com.mentify.enums.PaymentStatus;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentControllerTest {

    @Test
    void startCoursePayment_returnsCreatedApiResponse() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService);
        UUID courseId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String authorizationHeader = "Bearer student-token";
        StartCoursePaymentRequest request = StartCoursePaymentRequest.builder()
                .courseId(courseId)
                .build();
        StartCoursePaymentResponse serviceResponse = StartCoursePaymentResponse.builder()
                .paymentId(paymentId)
                .courseId(courseId)
                .amount(new BigDecimal("1000.00"))
                .currency("LKR")
                .status(PaymentStatus.PENDING)
                .clientSecret("client_secret")
                .build();
        when(paymentService.startCoursePayment(request, authorizationHeader)).thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<StartCoursePaymentResponse>> response =
                controller.startCoursePayment(request, authorizationHeader);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.getBody().getData()).isEqualTo(serviceResponse);
        verify(paymentService).startCoursePayment(request, authorizationHeader);
    }

    @Test
    void handleStripeWebhook_returnsAcceptedResponse() {
        PaymentService paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService);

        ResponseEntity<ApiResponse<Object>> response = controller.handleStripeWebhook("payload", "signature");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Stripe webhook accepted");
        verify(paymentService).handleStripeWebhook("payload", "signature");
    }
}
