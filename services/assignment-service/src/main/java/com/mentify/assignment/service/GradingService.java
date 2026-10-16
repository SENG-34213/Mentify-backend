package com.mentify.assignment.service;

import com.mentify.assignment.dto.request.GradeSubmissionRequest;
import com.mentify.assignment.dto.response.SubmissionResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.UUID;

public interface GradingService {
    ApiResponse<SubmissionResponse> gradeSubmission(UUID submissionId, GradeSubmissionRequest request, String authorizationHeader);
    ApiResponse<SubmissionResponse> returnSubmission(UUID submissionId, String authorizationHeader);
}
