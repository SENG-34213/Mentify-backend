package com.mentify.service;

import com.mentify.dto.AdminPaymentDetailResponse;
import com.mentify.dto.AdminPaymentFilter;
import com.mentify.dto.AdminPaymentSummaryResponse;
import com.mentify.dto.PaymentDetailResponse;
import com.mentify.dto.PaymentStatusResponse;
import com.mentify.dto.PaymentSummaryResponse;
import com.mentify.dto.PaymentVerificationResponse;
import com.mentify.dto.StartCoursePaymentRequest;
import com.mentify.dto.StartCoursePaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    StartCoursePaymentResponse startCoursePayment(StartCoursePaymentRequest request, String authorizationHeader);

    List<PaymentSummaryResponse> getCurrentStudentPayments();

    PaymentDetailResponse getCurrentStudentPayment(UUID paymentId);

    PaymentStatusResponse getCurrentStudentPaymentStatus(UUID paymentId);

    Page<AdminPaymentSummaryResponse> getAdminPayments(AdminPaymentFilter filter, Pageable pageable);

    AdminPaymentDetailResponse getAdminPayment(UUID paymentId);

    PaymentVerificationResponse verifySuccessfulPayment(UUID studentId, UUID courseId);

    void handleStripeWebhook(String payload, String signatureHeader);
}
