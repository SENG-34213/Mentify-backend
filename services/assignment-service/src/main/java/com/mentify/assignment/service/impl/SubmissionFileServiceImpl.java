package com.mentify.assignment.service.impl;

import com.mentify.assignment.client.MaterialServiceClient;
import com.mentify.assignment.client.MaterialInternalServiceClient;
import com.mentify.assignment.client.dto.MaterialDownloadResponse;
import com.mentify.assignment.client.dto.MaterialLookupResponse;
import com.mentify.assignment.client.dto.MaterialUploadResponse;
import com.mentify.assignment.client.dto.MaterialAssociationRequest;
import com.mentify.assignment.dto.request.AttachSubmissionMaterialRequest;
import com.mentify.assignment.dto.response.SubmissionFileDownloadResponse;
import com.mentify.assignment.dto.response.SubmissionFileResponse;
import com.mentify.assignment.entity.Assignment;
import com.mentify.assignment.entity.Submission;
import com.mentify.assignment.entity.SubmissionFile;
import com.mentify.assignment.enums.SubmissionStatus;
import com.mentify.assignment.repository.SubmissionFileRepository;
import com.mentify.assignment.repository.SubmissionRepository;
import com.mentify.assignment.service.SubmissionFileService;
import com.mentify.assignment.storage.FileStorageService;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.security.CurrentUserService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class SubmissionFileServiceImpl implements SubmissionFileService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionFileRepository fileRepository;
    private final FileStorageService fileStorageService;
    private final MaterialServiceClient materialServiceClient;
    private final MaterialInternalServiceClient materialInternalServiceClient;
    private final CurrentUserService currentUserService;

    public SubmissionFileServiceImpl(
            SubmissionRepository submissionRepository,
            SubmissionFileRepository fileRepository,
            FileStorageService fileStorageService,
            MaterialServiceClient materialServiceClient,
            CurrentUserService currentUserService
    ) {
        this(submissionRepository, fileRepository, fileStorageService, materialServiceClient, null, currentUserService);
    }

    @Autowired
    public SubmissionFileServiceImpl(
            SubmissionRepository submissionRepository,
            SubmissionFileRepository fileRepository,
            FileStorageService fileStorageService,
            MaterialServiceClient materialServiceClient,
            MaterialInternalServiceClient materialInternalServiceClient,
            CurrentUserService currentUserService
    ) {
        this.submissionRepository = submissionRepository;
        this.fileRepository = fileRepository;
        this.fileStorageService = fileStorageService;
        this.materialServiceClient = materialServiceClient;
        this.materialInternalServiceClient = materialInternalServiceClient;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional
    public ApiResponse<SubmissionFileResponse> uploadFile(
            UUID submissionId,
            MultipartFile file,
            String authorizationHeader
    ) {
        Submission submission = getSubmissionOrThrow(submissionId);
        assertStudentOwnsDraftSubmission(submission);
        UUID studentId = currentUserService.getCurrentUserId();
        UUID courseId = submission.getAssignment().getCourseId();

        ApiResponse<MaterialUploadResponse> upload = materialServiceClient.upload(
                file.getOriginalFilename() == null ? "Submission file" : file.getOriginalFilename(),
                null,
                "SUBMISSION_ATTACHMENT",
                courseId,
                file,
                authorizationHeader
        );
        if (upload == null || upload.getData() == null || upload.getData().id() == null) {
            throw new IllegalStateException("Material Service did not return an uploaded submission");
        }
        MaterialUploadResponse material = upload.getData();
        try {
            materialInternalServiceClient.associate(
                    material.id(),
                    new MaterialAssociationRequest(
                            "SUBMISSION", submission.getId(), courseId, studentId, "OWNER_ONLY"
                    ),
                    authorizationHeader
            );
            SubmissionFile saved = fileRepository.save(SubmissionFile.builder()
                    .submission(submission)
                    .materialId(material.id())
                    .storedFilename("material:" + material.id())
                    .originalFilename(material.originalFilename())
                    .contentType(material.contentType())
                    .fileSize(material.fileSize() == null ? 0 : material.fileSize())
                    .build());
            return response(HttpStatus.CREATED, "Submission file uploaded successfully", mapToResponse(saved));
        } catch (RuntimeException ex) {
            try {
                materialServiceClient.deleteMaterial(material.id(), authorizationHeader);
            } catch (RuntimeException ignored) {
                // Reconciliation can remove an unreferenced material later.
            }
            throw new IllegalStateException("Submission file association failed after upload", ex);
        }
    }

    @Override
    @Transactional
    public ApiResponse<SubmissionFileResponse> attachMaterial(
            UUID submissionId,
            AttachSubmissionMaterialRequest request,
            String authorizationHeader
    ) {
        Submission submission = getSubmissionOrThrow(submissionId);
        assertStudentOwnsDraftSubmission(submission);

        UUID currentStudentId = currentUserService.getCurrentUserId();
        MaterialLookupResponse material = loadMaterial(request.getMaterialId(), authorizationHeader);
        validateSubmissionMaterial(submission, material, currentStudentId);
        associateMaterialReference(submission, material.id(), currentStudentId, authorizationHeader);

        SubmissionFile file = SubmissionFile.builder()
                .submission(submission)
                .materialId(material.id())
                .storedFilename("material:" + material.id())
                .originalFilename(resolveOriginalFilename(material))
                .contentType(resolveContentType(material.contentType()))
                .fileSize(material.fileSize() == null ? 0 : material.fileSize())
                .build();

        SubmissionFile saved = fileRepository.save(file);
        return response(HttpStatus.CREATED, "Submission material attached successfully", mapToResponse(saved));
    }

    @Override
    public ApiResponse<List<SubmissionFileResponse>> listFiles(UUID submissionId, String authorizationHeader) {
        Submission submission = getSubmissionOrThrow(submissionId);
        assertCanAccessSubmissionFiles(submission);

        List<SubmissionFileResponse> files = fileRepository
                .findBySubmissionIdAndIsActiveTrueOrderByCreatedAtAsc(submissionId)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return response(HttpStatus.OK, "Submission files fetched successfully", files);
    }

    @Override
    public ApiResponse<SubmissionFileDownloadResponse> downloadFile(
            UUID submissionId,
            UUID fileId,
            String authorizationHeader
    ) {
        Submission submission = getSubmissionOrThrow(submissionId);
        assertCanAccessSubmissionFiles(submission);

        SubmissionFile file = fileRepository.findByIdAndSubmissionIdAndIsActiveTrue(fileId, submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("SubmissionFile", "id", fileId));

        if (file.isMaterialBacked()) {
            return response(
                    HttpStatus.OK,
                    "Submission file download URL generated",
                    mapMaterialDownload(file, authorizationHeader)
            );
        }

        return response(
                HttpStatus.OK,
                "Submission file downloaded successfully",
                mapLocalDownload(file)
        );
    }

    @Override
    @Transactional
    public ApiResponse<Object> deleteFile(UUID submissionId, UUID fileId, String authorizationHeader) {
        Submission submission = getSubmissionOrThrow(submissionId);
        assertStudentOwnsDraftSubmission(submission);

        SubmissionFile file = fileRepository.findByIdAndSubmissionIdAndIsActiveTrue(fileId, submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("SubmissionFile", "id", fileId));

        file.setActive(false);
        fileRepository.save(file);
        if (!file.isMaterialBacked() && file.getStoredFilename() != null) {
            fileStorageService.delete(file.getStoredFilename());
        }

        return response(HttpStatus.OK, "Submission file deleted successfully", null);
    }

    private Submission getSubmissionOrThrow(UUID submissionId) {
        return submissionRepository.findByIdAndIsActiveTrue(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", submissionId));
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

    private void associateMaterialReference(
            Submission submission, UUID materialId, UUID ownerId, String authorizationHeader) {
        if (materialInternalServiceClient == null) {
            return;
        }
        materialInternalServiceClient.associate(
                materialId,
                new MaterialAssociationRequest(
                        "SUBMISSION", submission.getId(), submission.getAssignment().getCourseId(), ownerId, "OWNER_ONLY"
                ),
                authorizationHeader
        );
    }

    private void validateSubmissionMaterial(
            Submission submission,
            MaterialLookupResponse material,
            UUID currentStudentId
    ) {
        Assignment assignment = submission.getAssignment();
        if (assignment == null || material.courseId() == null || !material.courseId().equals(assignment.getCourseId())) {
            throw new AccessDeniedException("Material does not belong to this submission course");
        }
        if (material.uploadedBy() == null || !material.uploadedBy().equals(currentStudentId)) {
            throw new AccessDeniedException("Students can only attach their own uploaded materials");
        }
    }

    private void assertStudentOwnsDraftSubmission(Submission submission) {
        if (!currentUserService.hasAnyRole("STUDENT")) {
            throw new AccessDeniedException("Access denied");
        }
        if (!currentUserService.getCurrentUserId().equals(submission.getStudentId())) {
            throw new AccessDeniedException("Students can only manage their own submission files");
        }
        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            throw new IllegalArgumentException("Submission files can only be changed while the submission is in DRAFT status");
        }
    }

    private void assertCanAccessSubmissionFiles(Submission submission) {
        if (currentUserService.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            return;
        }
        UUID currentUserId = currentUserService.getCurrentUserId();
        if (currentUserService.hasAnyRole("TEACHER")
                && submission.getAssignment() != null
                && currentUserId.equals(submission.getAssignment().getTeacherId())) {
            return;
        }
        if (currentUserService.hasAnyRole("STUDENT") && currentUserId.equals(submission.getStudentId())) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    private SubmissionFileDownloadResponse mapMaterialDownload(SubmissionFile file, String authorizationHeader) {
        MaterialDownloadResponse material = createMaterialDownload(file.getMaterialId(), authorizationHeader);
        return SubmissionFileDownloadResponse.builder()
                .id(file.getId())
                .submissionId(file.getSubmission().getId())
                .materialId(file.getMaterialId())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .downloadUrl(material.downloadUrl())
                .expiresAt(material.expiresAt())
                .build();
    }

    private SubmissionFileDownloadResponse mapLocalDownload(SubmissionFile file) {
        try (InputStream inputStream = fileStorageService.openStream(file.getStoredFilename())) {
            return SubmissionFileDownloadResponse.builder()
                    .id(file.getId())
                    .submissionId(file.getSubmission().getId())
                    .materialId(file.getMaterialId())
                    .originalFilename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getFileSize())
                    .content(inputStream.readAllBytes())
                    .build();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read submission file", ex);
        }
    }

    private SubmissionFileResponse mapToResponse(SubmissionFile file) {
        return SubmissionFileResponse.builder()
                .id(file.getId())
                .submissionId(file.getSubmission() != null ? file.getSubmission().getId() : null)
                .materialId(file.getMaterialId())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .materialBacked(file.isMaterialBacked())
                .createdAt(file.getCreatedAt())
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
