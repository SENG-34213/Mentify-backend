package com.mentify.assignment.service;

import com.mentify.assignment.dto.request.AttachSubmissionMaterialRequest;
import com.mentify.assignment.dto.response.SubmissionFileDownloadResponse;
import com.mentify.assignment.dto.response.SubmissionFileResponse;
import com.mentify.payload.response.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface SubmissionFileService {
    ApiResponse<SubmissionFileResponse> attachMaterial(UUID submissionId, AttachSubmissionMaterialRequest request, String authorizationHeader);
    ApiResponse<List<SubmissionFileResponse>> listFiles(UUID submissionId, String authorizationHeader);
    ApiResponse<SubmissionFileDownloadResponse> downloadFile(UUID submissionId, UUID fileId, String authorizationHeader);
    ApiResponse<Object> deleteFile(UUID submissionId, UUID fileId, String authorizationHeader);
}
