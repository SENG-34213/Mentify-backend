package com.mentify.service;

import com.mentify.dto.PaymentDetailResponse;
import com.mentify.dto.PaymentStatusResponse;
import com.mentify.dto.PaymentSummaryResponse;
import com.mentify.dto.StartCoursePaymentRequest;
import com.mentify.dto.StartCoursePaymentResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    StartCoursePaymentResponse startCoursePayment(StartCoursePaymentRequest request, String authorizationHeader);

    List<PaymentSummaryResponse> getCurrentStudentPayments();

    PaymentDetailResponse getCurrentStudentPayment(UUID paymentId);

    PaymentStatusResponse getCurrentStudentPaymentStatus(UUID paymentId);

    void handleStripeWebhook(String payload, String signatureHeader);
}
