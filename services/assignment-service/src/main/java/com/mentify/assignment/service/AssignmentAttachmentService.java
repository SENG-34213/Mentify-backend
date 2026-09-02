package com.mentify.assignment.service;

import com.mentify.assignment.dto.response.AssignmentAttachmentResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface AssignmentAttachmentService {
    ApiResponse<AssignmentAttachmentResponse> uploadAttachment(UUID assignmentId, MultipartFile file, String authorizationHeader);
    ApiResponse<List<AssignmentAttachmentResponse>> listAttachments(UUID assignmentId, String authorizationHeader);
    ApiResponse<byte[]> downloadAttachment(UUID assignmentId, UUID attachmentId, String authorizationHeader);
    ApiResponse<Object> deleteAttachment(UUID assignmentId, UUID attachmentId, String authorizationHeader);
}
