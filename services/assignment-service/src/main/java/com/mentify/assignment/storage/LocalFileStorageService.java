package com.mentify.assignment.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
// TODO: Replace local filesystem storage with S3 bucket upload later when the cloud setup is ready.
// Cloud push command for later: aws s3 cp ./uploads/assignment-files s3://<your-bucket-name>/assignment-files --recursive
// This is kept local for now; same TODO should be added in the course service when file upload is implemented there.
public class LocalFileStorageService implements FileStorageService {
    private final Path uploadDirectory;

    public LocalFileStorageService(
            @Value("${app.assignment.upload-dir:./uploads/assignment-files}")
            Path uploadDir) {

        this.uploadDirectory = Path.of(uploadDir.toUri());


        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not create upload directory: " + this.uploadDirectory,
                    e
            );
        }
    }
    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String originalName = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = getExtension(originalName);
        String storedFilename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path targetPath = uploadDirectory.resolve(storedFilename).normalize();

        if (!targetPath.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }

        return StoredFile.builder()
                .originalFilename(originalName)
                .storedFilename(storedFilename)
                .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .fileSize(file.getSize())
                .targetPath(targetPath)
                .build();
    }

    @Override
    public InputStream openStream(String storedFilename) {
        Path path = resolvePath(storedFilename);
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new IllegalStateException("File not found or inaccessible: " + storedFilename, e);
        }
    }

    @Override
    public void delete(String storedFilename) {
        Path path = resolvePath(storedFilename);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete file: " + storedFilename, e);
        }
    }

    @Override
    public Path resolvePath(String storedFilename) {
        String safeName = sanitizeOriginalFilename(storedFilename);
        Path resolved = uploadDirectory.resolve(safeName).normalize();
        if (!resolved.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        return resolved;
    }

    private String sanitizeOriginalFilename(String name) {
        if (name == null || name.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String cleaned = name.replace("\\", "/").replace("..", "");
        return cleaned.substring(Math.max(cleaned.lastIndexOf('/') + 1, 0));
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx > 0 && idx < filename.length() - 1) {
            return filename.substring(idx + 1);
        }
        return "";
    }
}
