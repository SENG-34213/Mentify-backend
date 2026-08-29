package com.mentify.service;

import com.mentify.dto.EntrollmentCreateRequest;
import com.mentify.dto.EntrollmentResponse;
import com.mentify.dto.EntrollmentUpdateRequest;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface EntrollmentService {

    ApiResponse<EntrollmentResponse> createEntrollment(EntrollmentCreateRequest request, String authorizationHeader);

    ApiResponse<EntrollmentResponse> updateEntrollment(UUID enrollmentId, EntrollmentUpdateRequest request, String authorizationHeader);
}

