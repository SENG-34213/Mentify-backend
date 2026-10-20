package com.mentify.exam.service;

import com.mentify.exam.dto.response.ExamAttachmentDownloadResponse;
import com.mentify.exam.dto.response.ExamAttachmentResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ExamAttachmentService {
    ApiResponse<ExamAttachmentResponse> upload(UUID examId, MultipartFile file, boolean studentVisible, String authorizationHeader);
    ApiResponse<List<ExamAttachmentResponse>> list(UUID examId, String authorizationHeader);
    ApiResponse<ExamAttachmentDownloadResponse> download(UUID examId, UUID attachmentId, String authorizationHeader);
    ApiResponse<Object> delete(UUID examId, UUID attachmentId, String authorizationHeader);
}
