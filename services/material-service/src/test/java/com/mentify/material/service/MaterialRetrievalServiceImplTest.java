package com.mentify.material.service;

import com.mentify.material.dto.response.MaterialDetailsResponse;
import com.mentify.material.dto.response.MaterialDownloadResponse;
import com.mentify.material.dto.response.S3DownloadResult;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import com.mentify.material.exception.MaterialNotFoundException;
import com.mentify.material.exception.MaterialUnavailableException;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.service.impl.MaterialRetrievalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialRetrievalServiceImplTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialStorageService storageService;

    @InjectMocks
    private MaterialRetrievalServiceImpl retrievalService;

    @Test
    void existingMaterialMetadataCanBeRetrievedWithoutStorageKeyExposure() {
        Material material = material(MaterialStatus.AVAILABLE);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));

        MaterialDetailsResponse response = retrievalService.getMaterial(material.getId());

        assertThat(response.id()).isEqualTo(material.getId());
        assertThat(response.title()).isEqualTo("Algebra intro");
        assertThat(response.originalFilename()).isEqualTo("intro.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.fileSize()).isEqualTo(12L);
        assertThat(response.status()).isEqualTo(MaterialStatus.AVAILABLE);
        assertThat(recordComponentNames(MaterialDetailsResponse.class)).doesNotContain("storageKey", "downloadUrl");
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

        MaterialDownloadResponse response = retrievalService.createDownload(material.getId());

        assertThat(response.downloadUrl()).isEqualTo("https://downloads.example.test/materials/object.pdf?X-Amz-Signature=fake");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        assertThat(response.downloadUrl()).doesNotContain("secret");
        assertThat(recordComponentNames(MaterialDownloadResponse.class)).doesNotContain("storageKey");
        verify(storageService).createPresignedDownload("materials/object.pdf", "intro.pdf", "application/pdf");
    }

    @Test
    void uploadedMaterialCanBeDownloadedBeforePostProcessingMarksItAvailable() {
        Material material = material(MaterialStatus.UPLOADED);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));
        when(storageService.createPresignedDownload("materials/object.pdf", "intro.pdf", "application/pdf"))
                .thenReturn(new S3DownloadResult("https://downloads.example.test/object", Instant.now()));

        MaterialDownloadResponse response = retrievalService.createDownload(material.getId());

        assertThat(response.status()).isEqualTo(MaterialStatus.UPLOADED);
    }

    @Test
    void missingMaterialReturnsSafeNotFoundError() {
        UUID materialId = UUID.randomUUID();
        when(materialRepository.findById(materialId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retrievalService.getMaterial(materialId))
                .isInstanceOf(MaterialNotFoundException.class)
                .hasMessage("Material not found");

        verifyNoInteractions(storageService);
    }

    @Test
    void deletedMaterialCannotBeRetrievedOrDownloaded() {
        Material material = material(MaterialStatus.DELETED);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> retrievalService.getMaterial(material.getId()))
                .isInstanceOf(MaterialNotFoundException.class)
                .hasMessage("Material not found");
        assertThatThrownBy(() -> retrievalService.createDownload(material.getId()))
                .isInstanceOf(MaterialNotFoundException.class)
                .hasMessage("Material not found");

        verifyNoInteractions(storageService);
    }

    @Test
    void inactiveMaterialCannotBeDownloaded() {
        Material material = material(MaterialStatus.AVAILABLE);
        material.setActive(false);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> retrievalService.createDownload(material.getId()))
                .isInstanceOf(MaterialNotFoundException.class)
                .hasMessage("Material not found");

        verifyNoInteractions(storageService);
    }

    @Test
    void unavailableMaterialStatusCannotBeDownloaded() {
        Material material = material(MaterialStatus.FAILED);
        when(materialRepository.findById(material.getId())).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> retrievalService.createDownload(material.getId()))
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
