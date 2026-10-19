package com.mentify.exam.controller;

import com.mentify.exam.dto.response.ExamAttachmentDownloadResponse;
import com.mentify.exam.dto.response.ExamAttachmentResponse;
import com.mentify.exam.service.ExamAttachmentService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exams/{examId}/attachments")
public class ExamAttachmentController {

    private final ExamAttachmentService attachmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ExamAttachmentResponse>> upload(
            @PathVariable UUID examId,
            @RequestPart MultipartFile file,
            @RequestParam(defaultValue = "false") boolean studentVisible,
            @RequestHeader("Authorization") String authorizationHeader) {
        ApiResponse<ExamAttachmentResponse> response = attachmentService.upload(
                examId, file, studentVisible, authorizationHeader);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ExamAttachmentResponse>>> list(
            @PathVariable UUID examId,
            @RequestHeader("Authorization") String authorizationHeader) {
        ApiResponse<List<ExamAttachmentResponse>> response = attachmentService.list(examId, authorizationHeader);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ExamAttachmentDownloadResponse>> download(
            @PathVariable UUID examId,
            @PathVariable UUID attachmentId,
            @RequestHeader("Authorization") String authorizationHeader) {
        ApiResponse<ExamAttachmentDownloadResponse> response = attachmentService.download(
                examId, attachmentId, authorizationHeader);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> delete(
            @PathVariable UUID examId,
            @PathVariable UUID attachmentId,
            @RequestHeader("Authorization") String authorizationHeader) {
        ApiResponse<Object> response = attachmentService.delete(examId, attachmentId, authorizationHeader);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
