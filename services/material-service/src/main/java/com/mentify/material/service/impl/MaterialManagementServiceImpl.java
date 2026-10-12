package com.mentify.material.service.impl;

import com.mentify.material.client.CourseServiceClient;
import com.mentify.material.client.dto.MaterialScopeLookupResponse;
import com.mentify.material.dto.request.MaterialSearchCriteria;
import com.mentify.material.dto.request.UpdateMaterialRequest;
import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialAuditAction;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.exception.MaterialNotFoundException;
import com.mentify.material.exception.MaterialUnavailableException;
import com.mentify.material.exception.MaterialValidationException;
import com.mentify.material.mapper.MaterialMapper;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.repository.MaterialSpecifications;
import com.mentify.material.security.MaterialAuthorizationService;
import com.mentify.material.service.MaterialAuditService;
import com.mentify.material.service.MaterialManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mentify.payload.response.ApiResponse;
import feign.FeignException;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final CourseServiceClient courseServiceClient;
    private final MaterialAuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<MaterialDetailsResponse> listMaterials(
            MaterialSearchCriteria criteria,
            String authorizationHeader,
            Pageable pageable
    ) {
        MaterialSearchCriteria resolvedCriteria = resolveCourseOwnedScope(criteria, authorizationHeader);
        validateSearchCriteria(resolvedCriteria);

        Specification<Material> visibleMaterials = MaterialSpecifications.visibleFilter(resolvedCriteria);
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
        Material saved = materialRepository.saveAndFlush(material);
        auditService.record(MaterialAuditAction.ARCHIVE, saved, null, "SUCCESS", null);
        return materialMapper.toDetailsResponse(saved);
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
        Material saved = materialRepository.saveAndFlush(material);
        auditService.record(MaterialAuditAction.DELETE, saved, null, "SUCCESS", null);
        return materialMapper.toDetailsResponse(saved);
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

    private MaterialSearchCriteria resolveCourseOwnedScope(
            MaterialSearchCriteria criteria,
            String authorizationHeader
    ) {
        MaterialSearchCriteria safeCriteria = criteria == null
                ? new MaterialSearchCriteria(null, null, null, null, null, null, null, null, null, null, null)
                : criteria;

        UUID resolvedCourseId = safeCriteria.courseId();
        Set<UUID> materialIds = null;

        if (safeCriteria.moduleId() != null) {
            MaterialScopeLookupResponse moduleScope = loadScope(
                    () -> courseServiceClient.getModuleMaterialScope(safeCriteria.moduleId(), authorizationHeader)
            );
            resolvedCourseId = mergeCourseScope(resolvedCourseId, moduleScope.courseId());
            materialIds = intersectMaterialIds(materialIds, moduleScope.materialIds());
        }

        if (safeCriteria.lessonId() != null) {
            MaterialScopeLookupResponse lessonScope = loadScope(
                    () -> courseServiceClient.getLessonMaterialScope(safeCriteria.lessonId(), authorizationHeader)
            );
            resolvedCourseId = mergeCourseScope(resolvedCourseId, lessonScope.courseId());
            materialIds = intersectMaterialIds(materialIds, lessonScope.materialIds());
        }

        return safeCriteria.withResolvedScope(resolvedCourseId, materialIds);
    }

    private UUID mergeCourseScope(UUID requestedCourseId, UUID ownerCourseId) {
        if (ownerCourseId == null) {
            throw new MaterialValidationException("Course-owned material scope could not be resolved");
        }
        if (requestedCourseId != null && !requestedCourseId.equals(ownerCourseId)) {
            throw new MaterialValidationException("Course id does not match the requested material scope");
        }
        return ownerCourseId;
    }

    private Set<UUID> intersectMaterialIds(Set<UUID> currentIds, List<UUID> nextIds) {
        Set<UUID> next = nextIds == null ? new HashSet<>() : new HashSet<>(nextIds);
        if (currentIds == null) {
            return next;
        }
        currentIds.retainAll(next);
        return currentIds;
    }

    private void validateSearchCriteria(MaterialSearchCriteria criteria) {
        if (criteria.createdFrom() != null
                && criteria.createdTo() != null
                && criteria.createdFrom().isAfter(criteria.createdTo())) {
            throw new MaterialValidationException("createdFrom must be before or equal to createdTo");
        }
    }

    private MaterialScopeLookupResponse loadScope(DownstreamScopeCall call) {
        try {
            ApiResponse<MaterialScopeLookupResponse> response = call.execute();
            if (response == null || response.getData() == null) {
                throw new MaterialValidationException("Material scope could not be resolved");
            }
            return response.getData();
        } catch (FeignException.Forbidden | FeignException.Unauthorized ex) {
            throw new AccessDeniedException("Material scope authorization was denied by course-service", ex);
        } catch (FeignException.NotFound ex) {
            throw new MaterialValidationException("Material scope could not be resolved");
        } catch (FeignException ex) {
            throw new AccessDeniedException("Material scope could not be verified", ex);
        }
    }

    @FunctionalInterface
    private interface DownstreamScopeCall {
        ApiResponse<MaterialScopeLookupResponse> execute();
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
