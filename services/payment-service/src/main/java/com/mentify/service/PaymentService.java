package com.mentify.service;

import com.mentify.dto.StartCoursePaymentRequest;
import com.mentify.dto.StartCoursePaymentResponse;

public interface PaymentService {

    StartCoursePaymentResponse startCoursePayment(StartCoursePaymentRequest request, String authorizationHeader);
}
