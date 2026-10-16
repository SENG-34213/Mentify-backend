package com.mentify.assignment.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalFileStorageServiceTest {

    @Test
    void uploadFile_whenValidFile_storesAndReturnsMetadata() {
        LocalFileStorageService storageService = new LocalFileStorageService(Path.of("target/test-uploads"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "hello world".getBytes()
        );

        StoredFile storedFile = storageService.store(file);

        assertThat(storedFile.getOriginalFilename()).isEqualTo("notes.txt");
        assertThat(storedFile.getStoredFilename()).isNotBlank();
    }

    @Test
    void uploadFile_whenFileIsEmpty_throwsIllegalArgumentException() {
        LocalFileStorageService storageService = new LocalFileStorageService(Path.of("target/test-uploads"));
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> storageService.store(file));
    }
}
