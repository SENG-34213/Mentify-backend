package com.mentify.exam.service.impl;

import com.mentify.exam.client.MaterialInternalServiceClient;
import com.mentify.exam.client.MaterialServiceClient;
import com.mentify.exam.client.EnrollmentServiceClient;
import com.mentify.exam.client.dto.MaterialAssociationRequest;
import com.mentify.exam.client.dto.MaterialDownloadResponse;
import com.mentify.exam.client.dto.MaterialUploadResponse;
import com.mentify.exam.dto.response.ExamAttachmentDownloadResponse;
import com.mentify.exam.dto.response.ExamAttachmentResponse;
import com.mentify.exam.entity.Exam;
import com.mentify.exam.entity.ExamAttachment;
import com.mentify.exam.enums.ExamRole;
import com.mentify.exam.enums.ExamStatus;
import com.mentify.exam.repository.ExamAttachmentRepository;
import com.mentify.exam.repository.ExamRepository;
import com.mentify.exam.security.CurrentUserService;
import com.mentify.exam.service.CourseAccessService;
import com.mentify.exam.service.ExamAttachmentService;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamAttachmentServiceImpl implements ExamAttachmentService {

    private final ExamRepository examRepository;
    private final ExamAttachmentRepository attachmentRepository;
    private final MaterialServiceClient materialServiceClient;
    private final MaterialInternalServiceClient materialInternalServiceClient;
    private final CourseAccessService courseAccessService;
    private final EnrollmentServiceClient enrollmentServiceClient;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ApiResponse<ExamAttachmentResponse> upload(UUID examId, MultipartFile file, boolean studentVisible,
                                                       String authorizationHeader) {
        Exam exam = findExam(examId);
        courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        assertEditable(exam);
        validateFile(file);
        UUID uploader = currentUserService.getCurrentUserId();

        ApiResponse<MaterialUploadResponse> result = materialServiceClient.upload(
                file.getOriginalFilename() == null ? "Exam attachment" : file.getOriginalFilename(),
                null, "EXAM_ATTACHMENT", exam.getCourseId(), file, authorizationHeader);
        if (result == null || result.getData() == null || result.getData().id() == null) {
            throw new IllegalStateException("Material Service did not return an uploaded exam attachment");
        }
        MaterialUploadResponse material = result.getData();
        try {
            materialInternalServiceClient.associate(
                    material.id(),
                    new MaterialAssociationRequest(
                            "EXAM", examId, exam.getCourseId(), uploader,
                            studentVisible ? "COURSE_MEMBERS" : "TEACHER_ONLY"),
                    authorizationHeader);
            ExamAttachment saved = attachmentRepository.save(ExamAttachment.builder()
                    .examId(examId)
                    .materialId(material.id())
                    .originalFilename(material.originalFilename() == null ? file.getOriginalFilename() : material.originalFilename())
                    .contentType(material.contentType() == null ? "application/octet-stream" : material.contentType())
                    .fileSize(material.fileSize() == null ? file.getSize() : material.fileSize())
                    .uploadedBy(uploader)
                    .studentVisible(studentVisible)
                    .active(true)
                    .build());
            List<ExamAttachment> previous = java.util.Optional.ofNullable(
                            attachmentRepository.findByExamIdAndActiveTrueOrderByCreatedAtAsc(examId))
                    .orElseGet(List::of).stream()
                    .filter(existing -> !existing.getId().equals(saved.getId()))
                    .peek(existing -> existing.setActive(false))
                    .toList();
            if (!previous.isEmpty()) {
                attachmentRepository.saveAll(previous);
            }
            return response(HttpStatus.CREATED, "Exam attachment uploaded successfully", toResponse(saved));
        } catch (RuntimeException ex) {
            try {
                materialServiceClient.deleteMaterial(material.id(), authorizationHeader);
            } catch (RuntimeException ignored) {
                // A reconciliation process can clean up an unreferenced material later.
            }
            throw new IllegalStateException("Exam attachment association failed after upload", ex);
        }
    }

    @Override
    @Transactional
    public ApiResponse<List<ExamAttachmentResponse>> list(UUID examId, String authorizationHeader) {
        Exam exam = findExam(examId);
        assertCanRead(exam, authorizationHeader);
        boolean student = currentUserService.hasAnyRole(ExamRole.STUDENT)
                && !currentUserService.hasAnyRole(ExamRole.TEACHER, ExamRole.ADMIN, ExamRole.SUPER_ADMIN);
        List<ExamAttachmentResponse> result = attachmentRepository
                .findByExamIdAndActiveTrueOrderByCreatedAtAsc(examId).stream()
                .filter(attachment -> !student || attachment.isStudentVisible())
                .map(this::toResponse).toList();
        return response(HttpStatus.OK, "Exam attachments fetched successfully", result);
    }

    @Override
    @Transactional
    public ApiResponse<ExamAttachmentDownloadResponse> download(UUID examId, UUID attachmentId,
                                                                 String authorizationHeader) {
        Exam exam = findExam(examId);
        assertCanRead(exam, authorizationHeader);
        ExamAttachment attachment = attachmentRepository.findByIdAndExamIdAndActiveTrue(attachmentId, examId)
                .orElseThrow(() -> new ResourceNotFoundException("ExamAttachment", "id", attachmentId));
        if (currentUserService.hasAnyRole(ExamRole.STUDENT)
                && !currentUserService.hasAnyRole(ExamRole.TEACHER, ExamRole.ADMIN, ExamRole.SUPER_ADMIN)
                && !attachment.isStudentVisible()) {
            throw new AccessDeniedException("Exam attachment is not released to students");
        }
        try {
            ApiResponse<MaterialDownloadResponse> result = materialServiceClient.createDownload(
                    attachment.getMaterialId(), authorizationHeader);
            MaterialDownloadResponse download = result == null ? null : result.getData();
            if (download == null || download.downloadUrl() == null) {
                throw new IllegalStateException("Material download URL is not available");
            }
            return response(HttpStatus.OK, "Exam attachment download URL generated",
                    ExamAttachmentDownloadResponse.builder()
                            .id(attachment.getId()).examId(examId).materialId(attachment.getMaterialId())
                            .originalFilename(attachment.getOriginalFilename()).contentType(attachment.getContentType())
                            .fileSize(attachment.getFileSize()).downloadUrl(download.downloadUrl())
                            .expiresAt(download.expiresAt()).build());
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Material", "id", attachment.getMaterialId());
        } catch (FeignException.Forbidden | FeignException.Unauthorized ex) {
            throw new AccessDeniedException("Exam attachment access denied", ex);
        }
    }

    @Override
    @Transactional
    public ApiResponse<Object> delete(UUID examId, UUID attachmentId, String authorizationHeader) {
        Exam exam = findExam(examId);
        courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
        assertEditable(exam);
        ExamAttachment attachment = attachmentRepository.findByIdAndExamIdAndActiveTrue(attachmentId, examId)
                .orElseThrow(() -> new ResourceNotFoundException("ExamAttachment", "id", attachmentId));
        attachment.setActive(false);
        attachmentRepository.save(attachment);
        // Material Service remains the owner of S3 cleanup and reference safety.
        return response(HttpStatus.OK, "Exam attachment deleted successfully", null);
    }

    private void assertCanRead(Exam exam, String authorizationHeader) {
        if (currentUserService.hasAnyRole(ExamRole.ADMIN, ExamRole.SUPER_ADMIN, ExamRole.TEACHER)) {
            courseAccessService.assertCanManage(exam.getCourseId(), authorizationHeader);
            return;
        }
        if (!currentUserService.hasAnyRole(ExamRole.STUDENT) || exam.getStatus() != ExamStatus.SCHEDULED
                || !isEnrolled(currentUserService.getCurrentUserId(), exam.getCourseId(), authorizationHeader)) {
            throw new AccessDeniedException("Exam materials are not available to this user");
        }
    }

    private boolean isEnrolled(UUID studentId, UUID courseId, String authorizationHeader) {
        try {
            return Boolean.TRUE.equals(enrollmentServiceClient.isStudentEnrolledInCourse(studentId, courseId));
        } catch (FeignException ex) {
            throw new AccessDeniedException("Enrollment could not be verified", ex);
        }
    }

    private Exam findExam(UUID examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", "id", examId));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Exam attachment file is required");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Exam instructions must be a PDF file");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !"application/pdf".equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("Exam instructions must be a PDF file");
        }
        if (file.getSize() > 25 * 1024 * 1024) {
            throw new IllegalArgumentException("Exam attachment exceeds the maximum allowed size");
        }
    }

    private void assertEditable(Exam exam) {
        if (exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.SCHEDULED) {
            throw new com.mentify.exam.exception.ExamDomainException(
                    HttpStatus.CONFLICT, "Exam in status " + exam.getStatus() + " cannot be modified");
        }
    }

    private ExamAttachmentResponse toResponse(ExamAttachment attachment) {
        return ExamAttachmentResponse.builder().id(attachment.getId()).examId(attachment.getExamId())
                .materialId(attachment.getMaterialId()).originalFilename(attachment.getOriginalFilename())
                .contentType(attachment.getContentType()).fileSize(attachment.getFileSize())
                .uploadedBy(attachment.getUploadedBy()).studentVisible(attachment.isStudentVisible())
                .createdAt(attachment.getCreatedAt()).build();
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder().status(status).statusCode(status.value()).message(message).data(data).build();
    }
}
