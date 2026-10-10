package com.mentify.material.service;

import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.dto.response.MaterialResponse;
import com.mentify.material.dto.response.S3UploadResult;
import com.mentify.material.entity.Material;
import com.mentify.material.enums.MaterialPurpose;
import com.mentify.material.enums.MaterialStatus;
import com.mentify.material.enums.MaterialType;
import com.mentify.material.exception.MaterialStorageException;
import com.mentify.material.exception.MaterialValidationException;
import com.mentify.material.repository.MaterialRepository;
import com.mentify.material.service.impl.MaterialUploadServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialUploadServiceImplTest {

    @Mock
    private MaterialFileValidationService validationService;

    @Mock
    private MaterialStorageService storageService;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private MaterialUploadServiceImpl uploadService;

    @Test
    void validFileUploadsToS3AndPersistsMetadataWithUploaderId() {
        UUID uploaderId = UUID.randomUUID();
        MultipartFile file = file();
        ValidatedMaterialFile validatedFile = validatedFile();
        when(validationService.validate(file)).thenReturn(validatedFile);
        when(storageService.upload(file, validatedFile)).thenReturn(new S3UploadResult("materials/object.pdf"));
        when(materialRepository.saveAndFlush(any(Material.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        MaterialResponse response = uploadService.upload(
                "  Algebra intro  ",
                "  Week 1 handout.  ",
                MaterialPurpose.LEARNING_MATERIAL,
                file,
                uploaderId
        );

        ArgumentCaptor<Material> materialCaptor = ArgumentCaptor.forClass(Material.class);
        verify(materialRepository).saveAndFlush(materialCaptor.capture());
        Material saved = materialCaptor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Algebra intro");
        assertThat(saved.getDescription()).isEqualTo("Week 1 handout.");
        assertThat(saved.getOriginalFilename()).isEqualTo("intro.pdf");
        assertThat(saved.getStorageKey()).isEqualTo("materials/object.pdf");
        assertThat(saved.getContentType()).isEqualTo("application/pdf");
        assertThat(saved.getFileSize()).isEqualTo(12L);
        assertThat(saved.getType()).isEqualTo(MaterialType.PDF);
        assertThat(saved.getPurpose()).isEqualTo(MaterialPurpose.LEARNING_MATERIAL);
        assertThat(saved.getStatus()).isEqualTo(MaterialStatus.UPLOADED);
        assertThat(saved.getUploadedBy()).isEqualTo(uploaderId);

        assertThat(response.storageKey()).isEqualTo("materials/object.pdf");
        assertThat(response.storageKey()).doesNotStartWith("http://").doesNotStartWith("https://");
        assertThat(response.uploadedBy()).isEqualTo(uploaderId);
    }

    @Test
    void invalidFileIsRejectedBeforeS3Upload() {
        MultipartFile file = file();
        when(validationService.validate(file)).thenThrow(new MaterialValidationException("Unsupported file type"));

        assertThatThrownBy(() -> uploadService.upload(
                "Notes",
                null,
                MaterialPurpose.LEARNING_MATERIAL,
                file,
                UUID.randomUUID()
        )).isInstanceOf(MaterialValidationException.class);

        verifyNoInteractions(storageService);
        verifyNoInteractions(materialRepository);
    }

    @Test
    void s3FailureDoesNotCreateMaterialRecord() {
        MultipartFile file = file();
        ValidatedMaterialFile validatedFile = validatedFile();
        when(validationService.validate(file)).thenReturn(validatedFile);
        when(storageService.upload(file, validatedFile)).thenThrow(new MaterialStorageException("Material file upload failed"));

        assertThatThrownBy(() -> uploadService.upload(
                "Notes",
                null,
                MaterialPurpose.LEARNING_MATERIAL,
                file,
                UUID.randomUUID()
        )).isInstanceOf(MaterialStorageException.class);

        verifyNoInteractions(materialRepository);
    }

    @Test
    void databaseFailureAfterS3UploadCleansUpUploadedObject() {
        MultipartFile file = file();
        ValidatedMaterialFile validatedFile = validatedFile();
        when(validationService.validate(file)).thenReturn(validatedFile);
        when(storageService.upload(file, validatedFile)).thenReturn(new S3UploadResult("materials/object.pdf"));
        when(materialRepository.saveAndFlush(any(Material.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> uploadService.upload(
                "Notes",
                null,
                MaterialPurpose.LEARNING_MATERIAL,
                file,
                UUID.randomUUID()
        )).isInstanceOf(DataIntegrityViolationException.class);

        verify(storageService).delete("materials/object.pdf");
    }

    @Test
    void invalidMetadataIsRejectedBeforeValidationAndUpload() {
        assertThatThrownBy(() -> uploadService.upload(
                " ",
                null,
                MaterialPurpose.LEARNING_MATERIAL,
                file(),
                UUID.randomUUID()
        )).isInstanceOf(MaterialValidationException.class)
                .hasMessage("Material title is required");

        verifyNoInteractions(validationService);
        verifyNoInteractions(storageService);
        verifyNoInteractions(materialRepository);
    }

    @Test
    void successfulPersistenceDoesNotDeleteUploadedObject() {
        UUID uploaderId = UUID.randomUUID();
        MultipartFile file = file();
        ValidatedMaterialFile validatedFile = validatedFile();
        when(validationService.validate(file)).thenReturn(validatedFile);
        when(storageService.upload(file, validatedFile)).thenReturn(new S3UploadResult("materials/object.pdf"));
        when(materialRepository.saveAndFlush(any(Material.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        uploadService.upload("Notes", null, null, file, uploaderId);

        verify(storageService, never()).delete(any());
    }

    private MultipartFile file() {
        return new MockMultipartFile("file", "intro.pdf", "application/pdf", "%PDF-1.7 data".getBytes());
    }

    private ValidatedMaterialFile validatedFile() {
        return new ValidatedMaterialFile(
                "intro.pdf",
                "intro.pdf",
                "pdf",
                "application/pdf",
                12L,
                MaterialType.PDF
        );
    }

    private Material persisted(Material material) {
        material.setId(UUID.randomUUID());
        material.setCreatedAt(LocalDateTime.of(2026, 10, 11, 8, 0));
        material.setUpdatedAt(LocalDateTime.of(2026, 10, 11, 8, 0));
        return material;
    }
}
