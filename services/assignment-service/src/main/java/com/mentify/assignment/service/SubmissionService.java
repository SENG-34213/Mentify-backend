package com.mentify.assignment.service;

import com.mentify.assignment.dto.request.CreateSubmissionRequest;
import com.mentify.assignment.dto.request.UpdateSubmissionRequest;
import com.mentify.assignment.dto.response.SubmissionResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface SubmissionService {
    ApiResponse<SubmissionResponse> createSubmission(UUID assignmentId, CreateSubmissionRequest request, String authorizationHeader);
    ApiResponse<SubmissionResponse> updateSubmission(UUID submissionId, UpdateSubmissionRequest request, String authorizationHeader);
    ApiResponse<SubmissionResponse> submitAssignment(UUID submissionId, String authorizationHeader);
    ApiResponse<SubmissionResponse> getSubmission(UUID submissionId, String authorizationHeader);
    ApiResponse<List<SubmissionResponse>> listSubmissionsForAssignment(UUID assignmentId, String authorizationHeader);
    ApiResponse<List<SubmissionResponse>> listMySubmissions(String authorizationHeader);
}
