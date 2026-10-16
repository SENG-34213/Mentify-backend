package com.mentify.assignment.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;

public interface FileStorageService {
    StoredFile store(MultipartFile file);
    InputStream openStream(String storedFilename);
    void delete(String storedFilename);
    Path resolvePath(String storedFilename);
}
