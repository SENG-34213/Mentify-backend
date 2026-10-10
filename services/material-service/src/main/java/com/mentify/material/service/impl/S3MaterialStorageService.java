package com.mentify.material.service.impl;

import com.mentify.material.config.S3Properties;
import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.dto.response.S3UploadResult;
import com.mentify.material.exception.MaterialStorageException;
import com.mentify.material.service.MaterialStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3MaterialStorageService implements MaterialStorageService {

    private static final String KEY_PREFIX = "materials";

    private final S3Client s3Client;
    private final S3Properties properties;

    @Override
    public S3UploadResult upload(MultipartFile file, ValidatedMaterialFile validatedFile) {
        String storageKey = generateStorageKey(validatedFile.extension());
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(requireBucket())
                    .key(storageKey)
                    .contentType(validatedFile.contentType())
                    .contentLength(validatedFile.fileSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), validatedFile.fileSize()));
            return new S3UploadResult(storageKey);
        } catch (IOException | SdkException ex) {
            log.warn("Material S3 upload failed for key={}: {}", storageKey, ex.getClass().getSimpleName());
            throw new MaterialStorageException("Material file upload failed", ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(requireBucket())
                    .key(storageKey)
                    .build();
            s3Client.deleteObject(request);
        } catch (SdkException ex) {
            log.error("Failed to clean up uploaded material object key={}", storageKey, ex);
        }
    }

    private String generateStorageKey(String extension) {
        return KEY_PREFIX + "/" + UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);
    }

    private String requireBucket() {
        String bucket = properties.bucket();
        if (bucket == null || bucket.isBlank()) {
            throw new MaterialStorageException("Material storage is not configured");
        }
        return bucket;
    }
}
