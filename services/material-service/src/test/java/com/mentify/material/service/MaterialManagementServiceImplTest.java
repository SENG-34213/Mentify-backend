package com.mentify.material.service;

import com.mentify.material.client.CourseServiceClient;
import com.mentify.material.client.dto.MaterialScopeLookupResponse;
import com.mentify.material.dto.request.MaterialSearchCriteria;
import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialAuditAction;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import com.mentify.material.exception.MaterialValidationException;
import com.mentify.material.mapper.MaterialMapper;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.security.MaterialAuthorizationService;
import com.mentify.material.service.impl.MaterialManagementServiceImpl;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialManagementServiceImplTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialAuthorizationService authorizationService;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private MaterialAuditService auditService;

    private MaterialManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MaterialManagementServiceImpl(
                materialRepository,
                new MaterialMapper(),
                authorizationService,
                courseServiceClient,
                auditService
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void listMaterialsResolvesModuleScopeThroughCourseService() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        Material material = material(materialId, courseId);
        Pageable pageable = PageRequest.of(0, 20);
        when(courseServiceClient.getModuleMaterialScope(moduleId, AUTH_HEADER))
                .thenReturn(scopeResponse(courseId, List.of(materialId)));
        when(authorizationService.visibilitySpecification(AUTH_HEADER)).thenReturn(null);
        when(materialRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(material), pageable, 1));

        Page<MaterialDetailsResponse> response = service.listMaterials(
                new MaterialSearchCriteria(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        moduleId,
                        null,
                        null,
                        null,
                        null
                ),
                AUTH_HEADER,
                pageable
        );

        assertThat(response.getContent()).extracting(MaterialDetailsResponse::id).containsExactly(materialId);
        verify(courseServiceClient).getModuleMaterialScope(moduleId, AUTH_HEADER);
        verify(materialRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listMaterialsRejectsConflictingCourseAndModuleScope() {
        UUID requestedCourseId = UUID.randomUUID();
        UUID moduleOwnerCourseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        when(courseServiceClient.getModuleMaterialScope(moduleId, AUTH_HEADER))
                .thenReturn(scopeResponse(moduleOwnerCourseId, List.of(UUID.randomUUID())));

        assertThatThrownBy(() -> service.listMaterials(
                new MaterialSearchCriteria(
                        null,
                        null,
                        null,
                        null,
                        null,
                        requestedCourseId,
                        moduleId,
                        null,
                        null,
                        null,
                        null
                ),
                AUTH_HEADER,
                PageRequest.of(0, 20)
        ))
                .isInstanceOf(MaterialValidationException.class)
                .hasMessage("Course id does not match the requested material scope");
    }

    @Test
    void archiveMaterialRecordsAuditEvent() {
        Material material = material(UUID.randomUUID(), UUID.randomUUID());
        when(materialRepository.findById(material.getId())).thenReturn(java.util.Optional.of(material));
        when(materialRepository.saveAndFlush(material)).thenReturn(material);

        MaterialDetailsResponse response = service.archiveMaterial(material.getId(), AUTH_HEADER);

        assertThat(response.status()).isEqualTo(MaterialStatus.ARCHIVED);
        verify(auditService).record(MaterialAuditAction.ARCHIVE, material, null, "SUCCESS", null);
    }

    @Test
    void deleteMaterialRecordsAuditEventWithoutPhysicalStorageDelete() {
        Material material = material(UUID.randomUUID(), UUID.randomUUID());
        when(materialRepository.findById(material.getId())).thenReturn(java.util.Optional.of(material));
        when(materialRepository.saveAndFlush(material)).thenReturn(material);

        MaterialDetailsResponse response = service.deleteMaterial(material.getId(), AUTH_HEADER);

        assertThat(response.status()).isEqualTo(MaterialStatus.DELETED);
        assertThat(material.isActive()).isFalse();
        verify(auditService).record(MaterialAuditAction.DELETE, material, null, "SUCCESS", null);
    }

    private ApiResponse<MaterialScopeLookupResponse> scopeResponse(UUID courseId, List<UUID> materialIds) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "ok",
                new MaterialScopeLookupResponse(courseId, materialIds)
        );
    }

    private Material material(UUID materialId, UUID courseId) {
        Material material = Material.builder()
                .title("Algebra intro")
                .description("Week 1 handout")
                .originalFilename("intro.pdf")
                .storageKey("materials/object.pdf")
                .contentType("application/pdf")
                .fileSize(12L)
                .type(MaterialType.PDF)
                .purpose(MaterialPurpose.LEARNING_MATERIAL)
                .status(MaterialStatus.AVAILABLE)
                .uploadedBy(UUID.randomUUID())
                .courseId(courseId)
                .checksum("sha256:abc")
                .build();
        material.setId(materialId);
        material.setCreatedAt(LocalDateTime.of(2026, 10, 11, 8, 0));
        material.setUpdatedAt(LocalDateTime.of(2026, 10, 11, 8, 1));
        material.setActive(true);
        return material;
    }
}
