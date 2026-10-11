package com.mentify.material.service.impl;

import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.dto.response.MaterialDownloadResponse;
import com.mentify.material.dto.response.S3DownloadResult;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.exception.MaterialNotFoundException;
import com.mentify.material.exception.MaterialUnavailableException;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.service.MaterialRetrievalService;
import com.mentify.material.service.MaterialStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialRetrievalServiceImpl implements MaterialRetrievalService {

    private static final EnumSet<MaterialStatus> DOWNLOADABLE_STATUSES = EnumSet.of(
            MaterialStatus.UPLOADED,
            MaterialStatus.AVAILABLE
    );

    private final MaterialRepository materialRepository;
    private final MaterialStorageService storageService;

    @Override
    public MaterialDetailsResponse getMaterial(UUID materialId) {
        Material material = loadVisibleMaterial(materialId);
        return toDetailsResponse(material);
    }

    @Override
    public MaterialDownloadResponse createDownload(UUID materialId) {
        Material material = loadVisibleMaterial(materialId);
        if (!DOWNLOADABLE_STATUSES.contains(material.getStatus())) {
            throw new MaterialUnavailableException("Material is not available for download");
        }

        S3DownloadResult download = storageService.createPresignedDownload(
                material.getStorageKey(),
                material.getOriginalFilename(),
                material.getContentType()
        );

        return new MaterialDownloadResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getOriginalFilename(),
                material.getContentType(),
                material.getFileSize(),
                material.getType(),
                material.getPurpose(),
                material.getStatus(),
                material.getUploadedBy(),
                material.getChecksum(),
                material.getCreatedAt(),
                material.getUpdatedAt(),
                download.downloadUrl(),
                download.expiresAt()
        );
    }

    private Material loadVisibleMaterial(UUID materialId) {
        if (materialId == null) {
            throw new MaterialNotFoundException("Material not found");
        }

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new MaterialNotFoundException("Material not found"));

        if (!material.isActive() || material.getStatus() == MaterialStatus.DELETED) {
            throw new MaterialNotFoundException("Material not found");
        }

        return material;
    }

    private MaterialDetailsResponse toDetailsResponse(Material material) {
        return new MaterialDetailsResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getOriginalFilename(),
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
