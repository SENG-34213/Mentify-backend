package com.mentify.material.service;

import com.mentify.material.config.MaterialValidationProperties;
import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.enums.MaterialType;
import com.mentify.material.exception.MaterialExceptionHandler;
import com.mentify.material.exception.MaterialValidationException;
import com.mentify.material.service.impl.MaterialFileValidationServiceImpl;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaterialFileValidationServiceImplTest {

    private MaterialValidationProperties properties;
    private MaterialFileValidationService validationService;

    @BeforeEach
    void setUp() {
        properties = new MaterialValidationProperties();
        properties.setMaxUploadSize(DataSize.ofKilobytes(10));
        validationService = new MaterialFileValidationServiceImpl(properties);
    }

    @Test
    void rejectsEmptyFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "lesson.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> validationService.validate(file))
                .isInstanceOf(MaterialValidationException.class)
                .hasMessage("File must not be empty");
    }

    @Test
    void rejectsOversizedFiles() {
        properties.setMaxUploadSize(DataSize.ofBytes(3));
        MockMultipartFile file = new MockMultipartFile(
                "file", "lesson.txt", "text/plain", "larger".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validationService.validate(file))
                .isInstanceOf(MaterialValidationException.class)
                .hasMessage("File exceeds the maximum allowed upload size");
    }

    @Test
    void rejectsUnsupportedExtensions() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.exe", "application/octet-stream", new byte[]{0x4D, 0x5A, 0x00, 0x00});

        assertThatThrownBy(() -> validationService.validate(file))
                .isInstanceOf(MaterialValidationException.class)
                .hasMessage("Unsupported file type");
    }

    @Test
    void rejectsInvalidContentTypesForAllowedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lesson.pdf", "text/plain", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> validationService.validate(file))
                .isInstanceOf(MaterialValidationException.class)
                .hasMessage("Unsupported file type");
    }

    @Test
    void rejectsFilesWhoseSignatureDoesNotMatchExtensionAndContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lesson.pdf", "application/pdf", "plain text".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validationService.validate(file))
                .isInstanceOf(MaterialValidationException.class)
                .hasMessage("File content does not match the declared file type");
    }

    @Test
    void rejectsUnsafePathBasedFilenames() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../lesson.pdf", "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> validationService.validate(file))
                .isInstanceOf(MaterialValidationException.class)
                .hasMessage("Filename is not safe");
    }

    @Test
    void sanitizesSafeOriginalFilenames() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                " Lecture notes (final).PDF ",
                "application/pdf; charset=binary",
                "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)
        );

        ValidatedMaterialFile validated = validationService.validate(file);

        assertThat(validated.originalFilename()).isEqualTo("Lecture notes (final).PDF");
        assertThat(validated.sanitizedFilename()).isEqualTo("Lecture_notes_final_.PDF");
        assertThat(validated.extension()).isEqualTo("pdf");
        assertThat(validated.contentType()).isEqualTo("application/pdf");
        assertThat(validated.materialType()).isEqualTo(MaterialType.PDF);
    }

    @Test
    void mapsSupportedFilesToMaterialTypes() {
        assertThat(validationService.validate(file("notes.txt", "text/plain", "Learning notes")).materialType())
                .isEqualTo(MaterialType.DOCUMENT);
        assertThat(validationService.validate(file("slides.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                new byte[]{0x50, 0x4B, 0x03, 0x04})).materialType())
                .isEqualTo(MaterialType.PRESENTATION);
        assertThat(validationService.validate(file("photo.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})).materialType())
                .isEqualTo(MaterialType.IMAGE);
        assertThat(validationService.validate(file("clip.mp4", "video/mp4",
                new byte[]{0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70})).materialType())
                .isEqualTo(MaterialType.VIDEO);
        assertThat(validationService.validate(file("audio.mp3", "audio/mpeg",
                new byte[]{0x49, 0x44, 0x33, 0x04})).materialType())
                .isEqualTo(MaterialType.AUDIO);
    }

    @Test
    void acceptsBrowserRecordedWebmAudio() {
        byte[] webmHeader = new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 0x42, (byte) 0x86};

        ValidatedMaterialFile validated = validationService.validate(
                file("voice.webm", "audio/webm;codecs=opus", webmHeader));

        assertThat(validated.materialType()).isEqualTo(MaterialType.AUDIO);
        assertThat(validated.contentType()).isEqualTo("audio/webm");
    }

    @Test
    void validationErrorResponsesUseSafeMessages() {
        MaterialExceptionHandler handler = new MaterialExceptionHandler();

        ResponseEntity<ApiResponse<Object>> response = handler.handleValidation(
                new MaterialValidationException("Unsupported file type"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Unsupported file type");
        assertThat(response.getBody().getMessage()).doesNotContain("script.exe");
    }

    @Test
    void missingRequiredUploadPartsReturnBadRequest() {
        MaterialExceptionHandler handler = new MaterialExceptionHandler();

        ResponseEntity<ApiResponse<Object>> response = handler.handleMissingRequestPart(
                new MissingServletRequestPartException("file"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Required multipart file or field is missing");
    }

    private MockMultipartFile file(String filename, String contentType, String content) {
        return file(filename, contentType, content.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile file(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("file", filename, contentType, content);
    }
}
