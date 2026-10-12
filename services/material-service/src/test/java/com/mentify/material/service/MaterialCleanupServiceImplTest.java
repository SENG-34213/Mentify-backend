package com.mentify.material.service;

import com.mentify.material.dto.request.MaterialCleanupRequest;
import com.mentify.material.dto.response.MaterialCleanupResponse;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.service.impl.MaterialCleanupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialCleanupServiceImplTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialStorageService storageService;

    @Mock
    private MaterialAuditService auditService;

    private MaterialCleanupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MaterialCleanupServiceImpl(materialRepository, storageService, auditService);
    }

    @Test
    void auditDetectsOrphanObjectsMissingObjectsAndIncompleteUploadsWithoutDeleting() {
        Material existing = material("materials/existing.pdf", MaterialStatus.AVAILABLE);
        Material missing = material("materials/missing.pdf", MaterialStatus.AVAILABLE);
        Material failed = material("materials/failed.pdf", MaterialStatus.FAILED);
        when(materialRepository.findAllStorageKeys()).thenReturn(List.of("materials/existing.pdf", "materials/missing.pdf"));
        when(storageService.listStorageKeys()).thenReturn(List.of("materials/existing.pdf", "materials/orphan.pdf"));
        when(materialRepository.findStorageTrackedMaterials()).thenReturn(List.of(existing, missing));
        when(storageService.exists("materials/existing.pdf")).thenReturn(true);
        when(storageService.exists("materials/missing.pdf")).thenReturn(false);
        when(materialRepository.findFailedOrIncompleteMaterials(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(failed));
        when(materialRepository.findPurgeableDeletedMaterials(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        MaterialCleanupResponse response = service.audit(new MaterialCleanupRequest(null, 30, 24));

        assertThat(response.dryRun()).isTrue();
        assertThat(response.orphanObjectCount()).isEqualTo(1);
        assertThat(response.missingObjectMaterialIds()).containsExactly(missing.getId());
        assertThat(response.failedOrIncompleteMaterialIds()).containsExactly(failed.getId());
        verify(storageService, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cleanupDeletesOnlyOrphansAndPurgeableDeletedMaterials() {
        Material active = material("materials/active.pdf", MaterialStatus.AVAILABLE);
        Material deleted = material("materials/deleted.pdf", MaterialStatus.DELETED);
        deleted.setActive(false);
        when(materialRepository.findAllStorageKeys()).thenReturn(List.of("materials/active.pdf", "materials/deleted.pdf"));
        when(storageService.listStorageKeys()).thenReturn(List.of("materials/active.pdf", "materials/deleted.pdf", "materials/orphan.pdf"));
        when(materialRepository.findStorageTrackedMaterials()).thenReturn(List.of(active));
        when(storageService.exists("materials/active.pdf")).thenReturn(true);
        when(materialRepository.findFailedOrIncompleteMaterials(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(materialRepository.findPurgeableDeletedMaterials(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(deleted));
        when(storageService.exists("materials/deleted.pdf")).thenReturn(true);

        MaterialCleanupResponse response = service.cleanup(new MaterialCleanupRequest(false, 30, 24));

        assertThat(response.dryRun()).isFalse();
        assertThat(response.orphanObjectsDeleted()).isEqualTo(1);
        assertThat(response.materialsPurged()).isEqualTo(1);
        assertThat(response.failures()).isEmpty();
        verify(storageService).delete("materials/orphan.pdf");
        verify(storageService).delete("materials/deleted.pdf");
        verify(materialRepository).delete(deleted);
        verify(storageService, never()).delete("materials/active.pdf");
    }

    @Test
    void cleanupCanBeRerunSafelyWhenNothingRemains() {
        when(materialRepository.findAllStorageKeys()).thenReturn(List.of());
        when(storageService.listStorageKeys()).thenReturn(List.of());
        when(materialRepository.findStorageTrackedMaterials()).thenReturn(List.of());
        when(materialRepository.findFailedOrIncompleteMaterials(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(materialRepository.findPurgeableDeletedMaterials(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        MaterialCleanupResponse response = service.cleanup(new MaterialCleanupRequest(false, 30, 24));

        assertThat(response.orphanObjectsDeleted()).isZero();
        assertThat(response.materialsPurged()).isZero();
        assertThat(response.failures()).isEmpty();
    }

    @Test
    void cleanupRecordsFailuresAndContinues() {
        Material deleted = material("materials/deleted.pdf", MaterialStatus.DELETED);
        deleted.setActive(false);
        when(materialRepository.findAllStorageKeys()).thenReturn(List.of("materials/deleted.pdf"));
        when(storageService.listStorageKeys()).thenReturn(List.of("materials/orphan.pdf"));
        when(materialRepository.findStorageTrackedMaterials()).thenReturn(List.of());
        when(materialRepository.findFailedOrIncompleteMaterials(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(materialRepository.findPurgeableDeletedMaterials(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(deleted));
        doThrow(new RuntimeException("s3 unavailable")).when(storageService).delete("materials/orphan.pdf");
        when(storageService.exists("materials/deleted.pdf")).thenReturn(false);

        MaterialCleanupResponse response = service.cleanup(new MaterialCleanupRequest(false, 30, 24));

        assertThat(response.failures()).hasSize(1);
        assertThat(response.materialsPurged()).isEqualTo(1);
        verify(materialRepository).delete(deleted);
    }

    private Material material(String storageKey, MaterialStatus status) {
        Material material = Material.builder()
                .title("Algebra intro")
                .description("Week 1 handout")
                .originalFilename("intro.pdf")
                .storageKey(storageKey)
                .contentType("application/pdf")
                .fileSize(12L)
                .type(MaterialType.PDF)
                .purpose(MaterialPurpose.LEARNING_MATERIAL)
                .status(status)
                .uploadedBy(UUID.randomUUID())
                .checksum("sha256:abc")
                .build();
        material.setId(UUID.randomUUID());
        material.setCreatedAt(LocalDateTime.of(2026, 10, 11, 8, 0));
        material.setUpdatedAt(LocalDateTime.of(2026, 10, 11, 8, 1));
        material.setActive(true);
        return material;
    }
}
