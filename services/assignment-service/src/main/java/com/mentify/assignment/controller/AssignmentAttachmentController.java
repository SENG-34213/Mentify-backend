package com.mentify.assignment.controller;

import com.mentify.assignment.dto.request.AttachAssignmentMaterialRequest;
import com.mentify.assignment.dto.response.AssignmentAttachmentDownloadResponse;
import com.mentify.assignment.dto.response.AssignmentAttachmentResponse;
import com.mentify.assignment.service.AssignmentAttachmentService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AssignmentAttachmentController {

    private final AssignmentAttachmentService attachmentService;

    @PostMapping("/assignments/{assignmentId}/attachments")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentAttachmentResponse>> attachMaterial(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody AttachAssignmentMaterialRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AssignmentAttachmentResponse> response =
                attachmentService.attachMaterial(assignmentId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/assignments/{assignmentId}/attachments")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<AssignmentAttachmentResponse>>> listAttachments(
            @PathVariable UUID assignmentId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<List<AssignmentAttachmentResponse>> response =
                attachmentService.listAttachments(assignmentId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/assignments/{assignmentId}/attachments/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AssignmentAttachmentDownloadResponse>> downloadAttachment(
            @PathVariable UUID assignmentId,
            @PathVariable UUID attachmentId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<AssignmentAttachmentDownloadResponse> response =
                attachmentService.downloadAttachment(assignmentId, attachmentId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/assignments/{assignmentId}/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteAttachment(
            @PathVariable UUID assignmentId,
            @PathVariable UUID attachmentId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<Object> response =
                attachmentService.deleteAttachment(assignmentId, attachmentId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
