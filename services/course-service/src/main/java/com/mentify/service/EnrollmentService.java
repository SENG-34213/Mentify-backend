package com.mentify.service;

import com.mentify.dto.EnrollmentRequest;
import com.mentify.dto.EnrollmentResponse;
import com.mentify.payload.response.ApiResponse;

public interface EnrollmentService {
    ApiResponse<EnrollmentResponse> createEnrollment(EnrollmentRequest request);
}
