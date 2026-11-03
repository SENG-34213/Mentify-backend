package com.mentify.material.service.impl;

import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.dto.response.MaterialDownloadResponse;
import com.mentify.material.dto.response.S3DownloadResult;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialAuditAction;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.exception.MaterialNotFoundException;
import com.mentify.material.exception.MaterialUnavailableException;
import com.mentify.material.mapper.MaterialMapper;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.security.MaterialAuthorizationService;
import com.mentify.material.service.MaterialAuditService;
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
    private final MaterialMapper materialMapper;
    private final MaterialAuthorizationService authorizationService;
    private final MaterialAuditService auditService;

    @Override
    public MaterialDetailsResponse getMaterial(UUID materialId, String authorizationHeader) {
        Material material = loadVisibleMaterial(materialId);
        authorizationService.assertCanView(material, authorizationHeader);
        return materialMapper.toDetailsResponse(material);
    }

    @Override
    public MaterialDownloadResponse createDownload(UUID materialId, String authorizationHeader) {
        Material material = loadVisibleMaterial(materialId);
        authorizationService.assertCanDownload(material, authorizationHeader);
        if (!DOWNLOADABLE_STATUSES.contains(material.getStatus())) {
            throw new MaterialUnavailableException("Material is not available for download");
        }

        S3DownloadResult download = storageService.createPresignedDownload(
                material.getStorageKey(),
                material.getOriginalFilename(),
                material.getContentType()
        );
        auditService.record(MaterialAuditAction.DOWNLOAD_ACCESS_GENERATED, material, null, "SUCCESS", null);

        return materialMapper.toDownloadResponse(material, download);
    }

    @Override
    public S3DownloadResult createPublicThumbnail(UUID materialId) {
        Material material = loadVisibleMaterial(materialId);
        if (material.getPurpose() != MaterialPurpose.MARKETPLACE_PRODUCT
                || material.getContentType() == null
                || !material.getContentType().toLowerCase().startsWith("image/")) {
            throw new MaterialNotFoundException("Marketplace thumbnail not found");
        }
        if (!DOWNLOADABLE_STATUSES.contains(material.getStatus())) {
            throw new MaterialUnavailableException("Marketplace thumbnail is not available");
        }
        return storageService.createPresignedDownload(
                material.getStorageKey(), material.getOriginalFilename(), material.getContentType());
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
}
