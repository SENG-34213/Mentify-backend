package com.mentify.controller;

import com.mentify.dto.AdminPaymentDetailResponse;
import com.mentify.dto.AdminPaymentFilter;
import com.mentify.dto.AdminPaymentSummaryResponse;
import com.mentify.dto.PaymentDetailResponse;
import com.mentify.dto.PaymentStatusResponse;
import com.mentify.dto.PaymentSummaryResponse;
import com.mentify.dto.StartCoursePaymentRequest;
import com.mentify.dto.StartCoursePaymentResponse;
import com.mentify.enums.PaymentStatus;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/course")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StartCoursePaymentResponse>> startCoursePayment(
            @Valid @RequestBody StartCoursePaymentRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        StartCoursePaymentResponse data = paymentService.startCoursePayment(request, authorizationHeader);
        ApiResponse<StartCoursePaymentResponse> response = ApiResponse.<StartCoursePaymentResponse>builder()
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .message("Course payment initiated")
                .data(data)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<PaymentSummaryResponse>>> getMyPayments() {
        List<PaymentSummaryResponse> data = paymentService.getCurrentStudentPayments();
        ApiResponse<List<PaymentSummaryResponse>> response = ApiResponse.<List<PaymentSummaryResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Student payments retrieved")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getMyPayment(
            @PathVariable UUID paymentId
    ) {
        PaymentDetailResponse data = paymentService.getCurrentStudentPayment(paymentId);
        ApiResponse<PaymentDetailResponse> response = ApiResponse.<PaymentDetailResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Student payment retrieved")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getMyPaymentStatus(
            @PathVariable UUID paymentId
    ) {
        PaymentStatusResponse data = paymentService.getCurrentStudentPaymentStatus(paymentId);
        ApiResponse<PaymentStatusResponse> response = ApiResponse.<PaymentStatusResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Student payment status retrieved")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<AdminPaymentSummaryResponse>>> getAdminPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(required = false) String reference,
            Pageable pageable
    ) {
        AdminPaymentFilter filter = AdminPaymentFilter.builder()
                .status(status)
                .studentId(studentId)
                .courseId(courseId)
                .createdFrom(createdFrom)
                .createdTo(createdTo)
                .reference(reference)
                .build();
        Page<AdminPaymentSummaryResponse> data = paymentService.getAdminPayments(filter, pageable);
        ApiResponse<Page<AdminPaymentSummaryResponse>> response = ApiResponse.<Page<AdminPaymentSummaryResponse>>builder()
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Admin payments retrieved")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdminPaymentDetailResponse>> getAdminPayment(
            @PathVariable UUID paymentId
    ) {
        AdminPaymentDetailResponse data = paymentService.getAdminPayment(paymentId);
        ApiResponse<AdminPaymentDetailResponse> response = ApiResponse.<AdminPaymentDetailResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Admin payment retrieved")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<ApiResponse<Object>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader
    ) {
        paymentService.handleStripeWebhook(payload, signatureHeader);
        ApiResponse<Object> response = ApiResponse.builder()
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .message("Stripe webhook accepted")
                .build();
        return ResponseEntity.ok(response);
    }
}
