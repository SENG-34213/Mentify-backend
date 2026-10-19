package com.mentify.assignment.controller;

import com.mentify.assignment.dto.request.AttachSubmissionMaterialRequest;
import com.mentify.assignment.dto.response.SubmissionFileDownloadResponse;
import com.mentify.assignment.dto.response.SubmissionFileResponse;
import com.mentify.assignment.service.SubmissionFileService;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SubmissionFileController {

    private final SubmissionFileService submissionFileService;

    @PostMapping(value = "/submissions/{submissionId}/files/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionFileResponse>> uploadFile(
            @PathVariable UUID submissionId,
            @RequestParam MultipartFile file,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<SubmissionFileResponse> response =
                submissionFileService.uploadFile(submissionId, file, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/submissions/{submissionId}/files")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionFileResponse>> attachMaterial(
            @PathVariable UUID submissionId,
            @Valid @RequestBody AttachSubmissionMaterialRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<SubmissionFileResponse> response =
                submissionFileService.attachMaterial(submissionId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/submissions/{submissionId}/files")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SubmissionFileResponse>>> listFiles(
            @PathVariable UUID submissionId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<List<SubmissionFileResponse>> response =
                submissionFileService.listFiles(submissionId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/submissions/{submissionId}/files/{fileId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SubmissionFileDownloadResponse>> downloadFile(
            @PathVariable UUID submissionId,
            @PathVariable UUID fileId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<SubmissionFileDownloadResponse> response =
                submissionFileService.downloadFile(submissionId, fileId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/submissions/{submissionId}/files/{fileId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Object>> deleteFile(
            @PathVariable UUID submissionId,
            @PathVariable UUID fileId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<Object> response = submissionFileService.deleteFile(submissionId, fileId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
