package com.mentify.material.service.impl;

import com.mentify.material.dto.request.UpdateMaterialRequest;
import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import com.mentify.material.exception.MaterialNotFoundException;
import com.mentify.material.exception.MaterialUnavailableException;
import com.mentify.material.exception.MaterialValidationException;
import com.mentify.material.mapper.MaterialMapper;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.repository.MaterialSpecifications;
import com.mentify.material.security.MaterialAuthorizationService;
import com.mentify.material.service.MaterialManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialManagementServiceImpl implements MaterialManagementService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final EnumSet<MaterialStatus> ARCHIVABLE_STATUSES = EnumSet.of(
            MaterialStatus.UPLOADED,
            MaterialStatus.AVAILABLE
    );
    private static final EnumSet<MaterialStatus> SOFT_DELETABLE_STATUSES = EnumSet.of(
            MaterialStatus.UPLOADED,
            MaterialStatus.AVAILABLE,
            MaterialStatus.ARCHIVED,
            MaterialStatus.FAILED
    );

    private final MaterialRepository materialRepository;
    private final MaterialMapper materialMapper;
    private final MaterialAuthorizationService authorizationService;

    @Override
    @Transactional(readOnly = true)
    public Page<MaterialDetailsResponse> listMaterials(
            MaterialType type,
            MaterialPurpose purpose,
            MaterialStatus status,
            UUID uploadedBy,
            UUID courseId,
            String authorizationHeader,
            Pageable pageable
    ) {
        Specification<Material> visibleMaterials = MaterialSpecifications.visibleFilter(
                type,
                purpose,
                status,
                uploadedBy,
                courseId
        );
        Specification<Material> authorizedMaterials = authorizationService.visibilitySpecification(authorizationHeader);
        if (authorizedMaterials != null) {
            visibleMaterials = visibleMaterials.and(authorizedMaterials);
        }

        return materialRepository.findAll(
                        visibleMaterials,
                        normalizePageable(pageable)
                )
                .map(materialMapper::toDetailsResponse);
    }

    @Override
    @Transactional
    public MaterialDetailsResponse updateMaterial(
            UUID materialId,
            UpdateMaterialRequest request,
            String authorizationHeader
    ) {
        Material material = loadVisibleMaterial(materialId);
        authorizationService.assertCanManage(material, authorizationHeader);
        validateUpdateRequest(request);

        if (request.title() != null) {
            material.setTitle(normalizeTitle(request.title()));
        }
        if (request.description() != null) {
            material.setDescription(normalizeDescription(request.description()));
        }

        return materialMapper.toDetailsResponse(materialRepository.saveAndFlush(material));
    }

    @Override
    @Transactional
    public MaterialDetailsResponse archiveMaterial(UUID materialId, String authorizationHeader) {
        Material material = loadVisibleMaterial(materialId);
        authorizationService.assertCanManage(material, authorizationHeader);
        if (!ARCHIVABLE_STATUSES.contains(material.getStatus())) {
            throw new MaterialUnavailableException("Material cannot be archived from status " + material.getStatus());
        }

        material.setStatus(MaterialStatus.ARCHIVED);
        return materialMapper.toDetailsResponse(materialRepository.saveAndFlush(material));
    }

    @Override
    @Transactional
    public MaterialDetailsResponse deleteMaterial(UUID materialId, String authorizationHeader) {
        Material material = loadVisibleMaterial(materialId);
        authorizationService.assertCanManage(material, authorizationHeader);
        if (!SOFT_DELETABLE_STATUSES.contains(material.getStatus())) {
            throw new MaterialUnavailableException("Material cannot be deleted from status " + material.getStatus());
        }

        material.setStatus(MaterialStatus.DELETED);
        material.setActive(false);
        return materialMapper.toDetailsResponse(materialRepository.saveAndFlush(material));
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

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, DEFAULT_SORT);
        }

        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT;
        return PageRequest.of(pageable.getPageNumber(), pageSize, sort);
    }

    private void validateUpdateRequest(UpdateMaterialRequest request) {
        if (request == null || (request.title() == null && request.description() == null)) {
            throw new MaterialValidationException("At least one material metadata field is required");
        }
        if (request.title() != null) {
            validateTitle(request.title());
        }
        if (request.description() != null && request.description().trim().length() > 2000) {
            throw new MaterialValidationException("Material description must not exceed 2000 characters");
        }
    }

    private void validateTitle(String title) {
        if (title.isBlank()) {
            throw new MaterialValidationException("Material title is required");
        }
        if (title.trim().length() > 200) {
            throw new MaterialValidationException("Material title must not exceed 200 characters");
        }
    }

    private String normalizeTitle(String title) {
        return title.trim();
    }

    private String normalizeDescription(String description) {
        if (description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
