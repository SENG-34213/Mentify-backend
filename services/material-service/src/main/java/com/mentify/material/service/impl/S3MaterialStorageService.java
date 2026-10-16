package com.mentify.material.service.impl;

import com.mentify.material.config.S3Properties;
import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.dto.response.S3DownloadResult;
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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3MaterialStorageService implements MaterialStorageService {

    private static final String KEY_PREFIX = "materials";
    private static final Duration DEFAULT_DOWNLOAD_URL_EXPIRATION = Duration.ofMinutes(5);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
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
            log.warn("Material S3 upload failed for keyHash={}: {}", keyHash(storageKey), ex.getClass().getSimpleName());
            throw new MaterialStorageException("Material file upload failed", ex);
        }
    }

    @Override
    public S3DownloadResult createPresignedDownload(String storageKey, String originalFilename, String contentType) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new MaterialStorageException("Material file is unavailable");
        }

        String bucket = requireBucket();
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build());

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .responseContentType(contentType)
                    .responseContentDisposition("attachment; filename=\"" + safeFilename(originalFilename) + "\"")
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(requireDownloadUrlExpiration())
                    .getObjectRequest(getObjectRequest)
                    .build());

            return new S3DownloadResult(presignedRequest.url().toString(), presignedRequest.expiration());
        } catch (S3Exception ex) {
            log.warn("Material S3 download presign failed for keyHash={} status={}", keyHash(storageKey), ex.statusCode());
            throw new MaterialStorageException("Material file is unavailable", ex);
        } catch (SdkException ex) {
            log.warn("Material S3 download presign failed for keyHash={}: {}", keyHash(storageKey), ex.getClass().getSimpleName());
            throw new MaterialStorageException("Material file is unavailable", ex);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(requireBucket())
                    .key(storageKey)
                    .build());
            return true;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return false;
            }
            log.warn("Material S3 existence check failed for keyHash={} status={}", keyHash(storageKey), ex.statusCode());
            throw new MaterialStorageException("Material storage could not be audited", ex);
        } catch (SdkException ex) {
            log.warn("Material S3 existence check failed for keyHash={}: {}", keyHash(storageKey), ex.getClass().getSimpleName());
            throw new MaterialStorageException("Material storage could not be audited", ex);
        }
    }

    @Override
    public List<String> listStorageKeys() {
        String bucket = requireBucket();
        List<String> keys = new ArrayList<>();
        String continuationToken = null;

        try {
            do {
                ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(KEY_PREFIX + "/")
                        .continuationToken(continuationToken)
                        .build());
                response.contents().forEach(object -> keys.add(object.key()));
                continuationToken = response.nextContinuationToken();
            } while (continuationToken != null);

            return keys;
        } catch (SdkException ex) {
            log.warn("Material S3 object listing failed: {}", ex.getClass().getSimpleName());
            throw new MaterialStorageException("Material storage could not be audited", ex);
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
            log.warn("Failed to delete material object keyHash={}: {}", keyHash(storageKey), ex.getClass().getSimpleName());
            throw new MaterialStorageException("Material file cleanup failed", ex);
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

    private Duration requireDownloadUrlExpiration() {
        Duration expiration = properties.downloadUrlExpiration();
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            return DEFAULT_DOWNLOAD_URL_EXPIRATION;
        }
        return expiration;
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "material";
        }
        return originalFilename.replace("\\", "_")
                .replace("\"", "_")
                .replace("\r", "_")
                .replace("\n", "_");
    }

    private String keyHash(String storageKey) {
        return Integer.toHexString(storageKey.hashCode());
    }
}
