package com.mentify.material.service;

import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.dto.response.MaterialDownloadResponse;
import com.mentify.material.dto.response.S3DownloadResult;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialAuditAction;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import com.mentify.material.exception.MaterialNotFoundException;
import com.mentify.material.exception.MaterialUnavailableException;
import com.mentify.material.mapper.MaterialMapper;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.security.MaterialAuthorizationService;
import com.mentify.material.service.impl.MaterialRetrievalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialRetrievalServiceImplTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialStorageService storageService;

    @Mock
    private MaterialAuthorizationService authorizationService;

    @Mock
    private MaterialAuditService auditService;

    private MaterialRetrievalServiceImpl retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new MaterialRetrievalServiceImpl(
                materialRepository,
                storageService,
                new MaterialMapper(),
                authorizationService,
                auditService
        );
    }

    @Test
    void existingMaterialMetadataCanBeRetrievedWithoutStorageKeyExposure() {
        Material material = material(MaterialStatus.AVAILABLE);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));

        MaterialDetailsResponse response = retrievalService.getMaterial(material.getId(), AUTH_HEADER);

        assertThat(response.id()).isEqualTo(material.getId());
        assertThat(response.title()).isEqualTo("Algebra intro");
        assertThat(response.originalFilename()).isEqualTo("intro.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.fileSize()).isEqualTo(12L);
        assertThat(response.status()).isEqualTo(MaterialStatus.AVAILABLE);
        assertThat(recordComponentNames(MaterialDetailsResponse.class)).doesNotContain("storageKey", "downloadUrl");
        verify(authorizationService).assertCanView(material, AUTH_HEADER);
        verifyNoInteractions(storageService);
    }

    @Test
    void validPresignedUrlIsGeneratedFromStoredStorageKeyOnly() {
        Material material = material(MaterialStatus.AVAILABLE);
        Instant expiresAt = Instant.parse("2026-10-11T05:30:00Z");
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(storageService.createPresignedDownload("materials/object.pdf", "intro.pdf", "application/pdf"))
                .thenReturn(new S3DownloadResult(
                        "https://downloads.example.test/materials/object.pdf?X-Amz-Signature=fake",
                        expiresAt
                ));

        MaterialDownloadResponse response = retrievalService.createDownload(material.getId(), AUTH_HEADER);

        assertThat(response.downloadUrl()).isEqualTo("https://downloads.example.test/materials/object.pdf?X-Amz-Signature=fake");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        assertThat(response.downloadUrl()).doesNotContain("secret");
        assertThat(recordComponentNames(MaterialDownloadResponse.class)).doesNotContain("storageKey");
        verify(authorizationService).assertCanDownload(material, AUTH_HEADER);
        verify(storageService).createPresignedDownload("materials/object.pdf", "intro.pdf", "application/pdf");
        verify(auditService).record(MaterialAuditAction.DOWNLOAD_ACCESS_GENERATED, material, null, "SUCCESS", null);
    }

    @Test
    void materialIdGuessingDoesNotBypassAuthorization() {
        Material material = material(MaterialStatus.AVAILABLE);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        doThrow(new AccessDeniedException("User is not authorized to download this material"))
                .when(authorizationService).assertCanDownload(material, AUTH_HEADER);

        assertThatThrownBy(() -> retrievalService.createDownload(material.getId(), AUTH_HEADER))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not authorized to download this material");

        verifyNoInteractions(storageService);
    }

    @Test
    void uploadedMaterialCanBeDownloadedBeforePostProcessingMarksItAvailable() {
        Material material = material(MaterialStatus.UPLOADED);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(storageService.createPresignedDownload("materials/object.pdf", "intro.pdf", "application/pdf"))
                .thenReturn(new S3DownloadResult("https://downloads.example.test/object", Instant.now()));

        MaterialDownloadResponse response = retrievalService.createDownload(material.getId(), AUTH_HEADER);

        assertThat(response.status()).isEqualTo(MaterialStatus.UPLOADED);
    }

    @Test
    void missingMaterialReturnsSafeNotFoundError() {
        UUID materialId = UUID.randomUUID();
        when(materialRepository.findById(materialId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retrievalService.getMaterial(materialId, AUTH_HEADER))
                .isInstanceOf(MaterialNotFoundException.class)
                .hasMessage("Material not found");

        verifyNoInteractions(storageService);
    }

    @Test
    void deletedMaterialCannotBeRetrievedOrDownloaded() {
        Material material = material(MaterialStatus.DELETED);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> retrievalService.getMaterial(material.getId(), AUTH_HEADER))
                .isInstanceOf(MaterialNotFoundException.class)
                .hasMessage("Material not found");
        assertThatThrownBy(() -> retrievalService.createDownload(material.getId(), AUTH_HEADER))
                .isInstanceOf(MaterialNotFoundException.class)
                .hasMessage("Material not found");

        verifyNoInteractions(storageService);
    }

    @Test
    void inactiveMaterialCannotBeDownloaded() {
        Material material = material(MaterialStatus.AVAILABLE);
        material.setActive(false);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> retrievalService.createDownload(material.getId(), AUTH_HEADER))
                .isInstanceOf(MaterialNotFoundException.class)
                .hasMessage("Material not found");

        verifyNoInteractions(storageService);
    }

    @Test
    void unavailableMaterialStatusCannotBeDownloaded() {
        Material material = material(MaterialStatus.FAILED);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> retrievalService.createDownload(material.getId(), AUTH_HEADER))
                .isInstanceOf(MaterialUnavailableException.class)
                .hasMessage("Material is not available for download");

        verifyNoInteractions(storageService);
    }

    private Material material(MaterialStatus status) {
        Material material = Material.builder()
                .title("Algebra intro")
                .description("Week 1 handout")
                .originalFilename("intro.pdf")
                .storageKey("materials/object.pdf")
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

    private String[] recordComponentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(RecordComponent::getName)
                .toArray(String[]::new);
    }
}
