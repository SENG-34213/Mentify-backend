package com.mentify.material.service.impl;

import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.dto.response.MaterialResponse;
import com.mentify.material.dto.response.S3UploadResult;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.exception.MaterialValidationException;
import com.mentify.material.repository.MaterialRepository;
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

    @Override
    public MaterialResponse upload(
            String title,
            String description,
            MaterialPurpose purpose,
            MultipartFile file,
            UUID uploadedBy
    ) {
        validateMetadata(title, description, uploadedBy);
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
                    .purpose(requirePurpose(purpose))
                    .status(MaterialStatus.UPLOADED)
                    .uploadedBy(uploadedBy)
                    .build();

            return toResponse(materialRepository.saveAndFlush(material));
        } catch (RuntimeException ex) {
            storageService.delete(uploadResult.storageKey());
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
                material.getChecksum(),
                material.getCreatedAt(),
                material.getUpdatedAt()
        );
    }
}
