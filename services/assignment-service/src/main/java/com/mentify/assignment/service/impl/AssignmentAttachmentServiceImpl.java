package com.mentify.assignment.service.impl;

import com.mentify.assignment.client.MaterialServiceClient;
import com.mentify.assignment.client.dto.MaterialDownloadResponse;
import com.mentify.assignment.client.dto.MaterialLookupResponse;
import com.mentify.assignment.dto.request.AttachAssignmentMaterialRequest;
import com.mentify.assignment.dto.response.AssignmentAttachmentDownloadResponse;
import com.mentify.assignment.dto.response.AssignmentAttachmentResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.AssignmentAttachment;
import com.mentify.assignment.enums.AssignmentStatus;
import com.mentify.assignment.repository.AssignmentAttachmentRepository;
import com.mentify.assignment.repository.AssignmentRepository;
import com.mentify.assignment.service.AssignmentAttachmentService;
import com.mentify.assignment.storage.FileStorageService;
import com.mentify.assignment.storage.StoredFile;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.CourseServiceClient;
import com.mentify.quiz.client.EnrollmentServiceClient;
import com.mentify.quiz.security.CurrentUserService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentAttachmentServiceImpl implements AssignmentAttachmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentAttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final MaterialServiceClient materialServiceClient;
    private final CourseServiceClient courseServiceClient;
    private final EnrollmentServiceClient enrollmentServiceClient;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ApiResponse<AssignmentAttachmentResponse> attachMaterial(
            UUID assignmentId,
            AttachAssignmentMaterialRequest request,
            String authorizationHeader
    ) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertTeacherCanManageAssignment(assignment);
        validateCourseExists(assignment.getCourseId(), authorizationHeader);

        MaterialLookupResponse material = loadMaterial(request.getMaterialId(), authorizationHeader);
        if (material.courseId() == null || !material.courseId().equals(assignment.getCourseId())) {
            throw new AccessDeniedException("Material does not belong to this assignment course");
        }

        AssignmentAttachment attachment = AssignmentAttachment.builder()
                .assignment(assignment)
                .materialId(material.id())
                .storedFilename("material:" + material.id())
                .originalFilename(resolveOriginalFilename(material))
                .contentType(resolveContentType(material.contentType()))
                .fileSize(material.fileSize() == null ? 0 : material.fileSize())
                .uploadedBy(currentUserService.getCurrentUserId())
                .build();

        AssignmentAttachment saved = attachmentRepository.save(attachment);
        return response(HttpStatus.CREATED, "Assignment material attached successfully", mapToResponse(saved));
    }

    @Override
    @Transactional
    public ApiResponse<AssignmentAttachmentResponse> uploadAttachment(
            UUID assignmentId,
            MultipartFile file,
            String authorizationHeader
    ) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertTeacherCanManageAssignment(assignment);
        validateCourseExists(assignment.getCourseId(), authorizationHeader);

        StoredFile storedFile = fileStorageService.store(file);
        AssignmentAttachment attachment = AssignmentAttachment.builder()
                .assignment(assignment)
                .originalFilename(storedFile.getOriginalFilename())
                .storedFilename(storedFile.getStoredFilename())
                .contentType(storedFile.getContentType())
                .fileSize(storedFile.getFileSize())
                .uploadedBy(currentUserService.getCurrentUserId())
                .build();

        AssignmentAttachment saved = attachmentRepository.save(attachment);
        return response(HttpStatus.CREATED, "Assignment attachment uploaded successfully", mapToResponse(saved));
    }

    @Override
    public ApiResponse<List<AssignmentAttachmentResponse>> listAttachments(UUID assignmentId, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertCanReadAssignmentAttachments(assignment, authorizationHeader);

        List<AssignmentAttachmentResponse> attachments = attachmentRepository
                .findByAssignmentIdAndIsActiveTrueOrderByCreatedAtAsc(assignmentId)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return response(HttpStatus.OK, "Assignment attachments fetched successfully", attachments);
    }

    @Override
    public ApiResponse<AssignmentAttachmentDownloadResponse> downloadAttachment(
            UUID assignmentId,
            UUID attachmentId,
            String authorizationHeader
    ) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertCanReadAssignmentAttachments(assignment, authorizationHeader);

        AssignmentAttachment attachment = attachmentRepository
                .findByIdAndAssignmentIdAndIsActiveTrue(attachmentId, assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("AssignmentAttachment", "id", attachmentId));

        if (attachment.isMaterialBacked()) {
            return response(
                    HttpStatus.OK,
                    "Assignment attachment download URL generated",
                    mapMaterialDownload(attachment, authorizationHeader)
            );
        }

        return response(
                HttpStatus.OK,
                "Assignment attachment downloaded successfully",
                mapLocalDownload(attachment)
        );
    }

    @Override
    @Transactional
    public ApiResponse<Object> deleteAttachment(UUID assignmentId, UUID attachmentId, String authorizationHeader) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        assertTeacherCanManageAssignment(assignment);
        validateCourseExists(assignment.getCourseId(), authorizationHeader);

        AssignmentAttachment attachment = attachmentRepository
                .findByIdAndAssignmentIdAndIsActiveTrue(attachmentId, assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("AssignmentAttachment", "id", attachmentId));

        attachment.setActive(false);
        attachmentRepository.save(attachment);
        if (!attachment.isMaterialBacked() && attachment.getStoredFilename() != null) {
            fileStorageService.delete(attachment.getStoredFilename());
        }

        return response(HttpStatus.OK, "Assignment attachment deleted successfully", null);
    }

    private Assignment getAssignmentOrThrow(UUID assignmentId) {
        return assignmentRepository.findByIdAndIsActiveTrue(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));
    }

    private void validateCourseExists(UUID courseId, String authorizationHeader) {
        try {
            ApiResponse<?> response = courseServiceClient.getCourseById(courseId, authorizationHeader);
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException("Course", "id", courseId);
            }
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Course", "id", courseId);
        } catch (FeignException.Forbidden | FeignException.Unauthorized ex) {
            throw new AccessDeniedException("Course access denied", ex);
        }
    }

    private MaterialLookupResponse loadMaterial(UUID materialId, String authorizationHeader) {
        try {
            ApiResponse<MaterialLookupResponse> response = materialServiceClient.getMaterial(materialId, authorizationHeader);
            if (response == null || response.getData() == null || response.getData().id() == null) {
                throw new ResourceNotFoundException("Material", "id", materialId);
            }
            return response.getData();
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Material", "id", materialId);
        } catch (FeignException.Forbidden | FeignException.Unauthorized ex) {
            throw new AccessDeniedException("Material access denied", ex);
        }
    }

    private MaterialDownloadResponse createMaterialDownload(UUID materialId, String authorizationHeader) {
        try {
            ApiResponse<MaterialDownloadResponse> response = materialServiceClient.createDownload(materialId, authorizationHeader);
            if (response == null || response.getData() == null || response.getData().downloadUrl() == null) {
                throw new IllegalStateException("Material download URL is not available");
            }
            return response.getData();
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Material", "id", materialId);
        } catch (FeignException.Forbidden | FeignException.Unauthorized ex) {
            throw new AccessDeniedException("Material access denied", ex);
        }
    }

    private void assertTeacherCanManageAssignment(Assignment assignment) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        if (!currentUserService.hasAnyRole("TEACHER")) {
            throw new AccessDeniedException("Access denied");
        }
        if (!currentUserService.getCurrentUserId().equals(assignment.getTeacherId())) {
            throw new AccessDeniedException("Teacher can only manage their own assignments");
        }
    }

    private void assertCanReadAssignmentAttachments(Assignment assignment, String authorizationHeader) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        UUID currentUserId = currentUserService.getCurrentUserId();
        if (currentUserService.hasAnyRole("TEACHER") && currentUserId.equals(assignment.getTeacherId())) {
            return;
        }
        if (currentUserService.hasAnyRole("STUDENT")
                && assignment.getStatus() == AssignmentStatus.PUBLISHED
                && isStudentEnrolled(currentUserId, assignment.getCourseId(), authorizationHeader)) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    private boolean isStudentEnrolled(UUID studentId, UUID courseId, String authorizationHeader) {
        try {
            return enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId, authorizationHeader);
        } catch (FeignException.Forbidden | FeignException.Unauthorized ex) {
            throw new AccessDeniedException("Enrollment access denied", ex);
        } catch (FeignException.NotFound ex) {
            return false;
        }
    }

    private AssignmentAttachmentDownloadResponse mapMaterialDownload(
            AssignmentAttachment attachment,
            String authorizationHeader
    ) {
        MaterialDownloadResponse material = createMaterialDownload(attachment.getMaterialId(), authorizationHeader);
        return AssignmentAttachmentDownloadResponse.builder()
                .id(attachment.getId())
                .assignmentId(attachment.getAssignment().getId())
                .materialId(attachment.getMaterialId())
                .originalFilename(attachment.getOriginalFilename())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .downloadUrl(material.downloadUrl())
                .expiresAt(material.expiresAt())
                .build();
    }

    private AssignmentAttachmentDownloadResponse mapLocalDownload(AssignmentAttachment attachment) {
        try (InputStream inputStream = fileStorageService.openStream(attachment.getStoredFilename())) {
            return AssignmentAttachmentDownloadResponse.builder()
                    .id(attachment.getId())
                    .assignmentId(attachment.getAssignment().getId())
                    .materialId(attachment.getMaterialId())
                    .originalFilename(attachment.getOriginalFilename())
                    .contentType(attachment.getContentType())
                    .fileSize(attachment.getFileSize())
                    .content(inputStream.readAllBytes())
                    .build();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read assignment attachment", ex);
        }
    }

    private AssignmentAttachmentResponse mapToResponse(AssignmentAttachment attachment) {
        return AssignmentAttachmentResponse.builder()
                .id(attachment.getId())
                .assignmentId(attachment.getAssignment() != null ? attachment.getAssignment().getId() : null)
                .materialId(attachment.getMaterialId())
                .originalFilename(attachment.getOriginalFilename())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .materialBacked(attachment.isMaterialBacked())
                .createdAt(attachment.getCreatedAt())
                .build();
    }

    private String resolveOriginalFilename(MaterialLookupResponse material) {
        if (material.originalFilename() != null && !material.originalFilename().isBlank()) {
            return material.originalFilename();
        }
        if (material.title() != null && !material.title().isBlank()) {
            return material.title();
        }
        return material.id().toString();
    }

    private String resolveContentType(String contentType) {
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .statusCode(status.value())
                .message(message)
                .data(data)
                .build();
    }
}
