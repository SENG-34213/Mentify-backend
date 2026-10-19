package com.mentify.material.service.impl;

import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.dto.response.MaterialResponse;
import com.mentify.material.dto.response.S3UploadResult;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialAuditAction;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialResourceType;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialVisibility;
import com.mentify.material.exception.MaterialValidationException;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.security.MaterialAuthorizationService;
import com.mentify.material.service.MaterialAuditService;
import com.mentify.material.service.MaterialFileValidationService;
import com.mentify.material.service.MaterialStorageService;
import com.mentify.material.service.MaterialUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialUploadServiceImpl implements MaterialUploadService {

    private final MaterialFileValidationService validationService;
    private final MaterialStorageService storageService;
    private final MaterialRepository materialRepository;
    private final MaterialAuthorizationService authorizationService;
    private final MaterialAuditService auditService;

    @Override
    public MaterialResponse upload(
            String title,
            String description,
            MaterialPurpose purpose,
            MultipartFile file,
            UUID uploadedBy,
            UUID courseId,
            String authorizationHeader
    ) {
        validateMetadata(title, description, uploadedBy);
        MaterialPurpose resolvedPurpose = requirePurpose(purpose);
        UUID authorizedCourseId = authorizationService.requireUploadCourseAccess(courseId, resolvedPurpose, authorizationHeader);
        ValidatedMaterialFile validatedFile = validationService.validate(file);
        S3UploadResult uploadResult = storageService.upload(file, validatedFile);

        try {
            Material material = Material.builder()
                    .title(normalizeTitle(title))
                    .description(normalizeDescription(description))
                    .originalFilename(validatedFile.originalFilename())
                    .storageKey(uploadResult.storageKey())
                    .contentType(validatedFile.contentType())
                    .fileSize(validatedFile.fileSize())
                    .type(validatedFile.materialType())
                    .purpose(resolvedPurpose)
                    .status(MaterialStatus.UPLOADED)
                    .uploadedBy(uploadedBy)
                    .courseId(authorizedCourseId)
                    .resourceType(MaterialResourceType.COURSE)
                    .resourceId(authorizedCourseId)
                    .ownerId(uploadedBy)
                    .visibility(resolvedPurpose == MaterialPurpose.SUBMISSION_ATTACHMENT
                            ? MaterialVisibility.OWNER_ONLY
                            : MaterialVisibility.COURSE_MEMBERS)
                    .build();

            Material saved = materialRepository.saveAndFlush(material);
            auditService.record(MaterialAuditAction.UPLOAD, saved, uploadedBy, "SUCCESS", null);
            return toResponse(saved);
        } catch (RuntimeException ex) {
            safeDeleteUploadedObject(uploadResult.storageKey());
            throw ex;
        }
    }

    private void validateMetadata(String title, String description, UUID uploadedBy) {
        if (uploadedBy == null) {
            throw new MaterialValidationException("Authenticated uploader is required");
        }
        if (title == null || title.isBlank()) {
            throw new MaterialValidationException("Material title is required");
        }
        if (title.trim().length() > 200) {
            throw new MaterialValidationException("Material title must not exceed 200 characters");
        }
        if (description != null && description.trim().length() > 2000) {
            throw new MaterialValidationException("Material description must not exceed 2000 characters");
        }
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        return title.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private MaterialPurpose requirePurpose(MaterialPurpose purpose) {
        return purpose == null ? MaterialPurpose.LEARNING_MATERIAL : purpose;
    }

    private void safeDeleteUploadedObject(String storageKey) {
        try {
            storageService.delete(storageKey);
        } catch (RuntimeException ignored) {
            // Upload rollback cleanup must not mask the original persistence failure.
        }
    }

    private MaterialResponse toResponse(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getOriginalFilename(),
                material.getStorageKey(),
                material.getContentType(),
                material.getFileSize(),
                material.getType(),
                material.getPurpose(),
                material.getStatus(),
                material.getUploadedBy(),
                material.getCourseId(),
                material.getChecksum(),
                material.getCreatedAt(),
                material.getUpdatedAt()
        );
    }
}
