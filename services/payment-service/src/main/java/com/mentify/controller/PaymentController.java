package com.mentify.controller;

import com.mentify.dto.StartCoursePaymentRequest;
import com.mentify.dto.StartCoursePaymentResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
