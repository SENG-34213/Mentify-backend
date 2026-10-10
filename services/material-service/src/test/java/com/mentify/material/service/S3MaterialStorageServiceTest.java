package com.mentify.material.service;

import com.mentify.material.config.S3Properties;
import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.dto.response.S3UploadResult;
import com.mentify.material.enums.MaterialType;
import com.mentify.material.exception.MaterialStorageException;
import com.mentify.material.service.impl.S3MaterialStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3MaterialStorageServiceTest {

    @Test
    void uploadsFileToPrivateBucketWithUniqueUuidBasedStorageKey() {
        RecordingS3Client recordingS3Client = new RecordingS3Client();
        S3MaterialStorageService storageService = storageService(recordingS3Client);

        S3UploadResult first = storageService.upload(file(), validatedFile());
        S3UploadResult second = storageService.upload(file(), validatedFile());

        List<PutObjectRequest> requests = recordingS3Client.putRequests;
        assertThat(requests).allSatisfy(request -> {
            assertThat(request.bucket()).isEqualTo("material-service-test-bucket");
            assertThat(request.key()).matches("materials/[0-9a-fA-F-]{36}\\.pdf");
            assertThat(request.contentType()).isEqualTo("application/pdf");
            assertThat(request.contentLength()).isEqualTo(12L);
            assertThat(request.aclAsString()).isNull();
        });
        assertThat(first.storageKey()).isNotEqualTo(second.storageKey());
        assertThat(first.storageKey()).doesNotStartWith("http://").doesNotStartWith("https://");
    }

    @Test
    void s3FailureThrowsSafeStorageException() {
        RecordingS3Client recordingS3Client = new RecordingS3Client();
        recordingS3Client.putFailure = S3Exception.builder().message("secret aws detail").build();
        S3MaterialStorageService storageService = storageService(recordingS3Client);

        assertThatThrownBy(() -> storageService.upload(file(), validatedFile()))
                .isInstanceOf(MaterialStorageException.class)
                .hasMessage("Material file upload failed");
    }

    @Test
    void deletesUploadedObjectByStorageKey() {
        RecordingS3Client recordingS3Client = new RecordingS3Client();
        S3MaterialStorageService storageService = storageService(recordingS3Client);

        storageService.delete("materials/object.pdf");

        assertThat(recordingS3Client.deleteRequests).hasSize(1);
        assertThat(recordingS3Client.deleteRequests.get(0).bucket()).isEqualTo("material-service-test-bucket");
        assertThat(recordingS3Client.deleteRequests.get(0).key()).isEqualTo("materials/object.pdf");
    }

    private S3MaterialStorageService storageService(RecordingS3Client recordingS3Client) {
        return new S3MaterialStorageService(
                recordingS3Client.client(),
                new S3Properties("material-service-test-bucket", "us-east-1")
        );
    }

    private MockMultipartFile file() {
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

    private static class RecordingS3Client implements InvocationHandler {

        private final List<PutObjectRequest> putRequests = new ArrayList<>();
        private final List<DeleteObjectRequest> deleteRequests = new ArrayList<>();
        private RuntimeException putFailure;

        private S3Client client() {
            return (S3Client) Proxy.newProxyInstance(
                    S3Client.class.getClassLoader(),
                    new Class[]{S3Client.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "putObject" -> {
                    if (putFailure != null) {
                        throw putFailure;
                    }
                    putRequests.add((PutObjectRequest) args[0]);
                    yield PutObjectResponse.builder().eTag("etag").build();
                }
                case "deleteObject" -> {
                    deleteRequests.add((DeleteObjectRequest) args[0]);
                    yield DeleteObjectResponse.builder().build();
                }
                case "serviceName" -> "s3";
                case "close" -> null;
                case "toString" -> "RecordingS3Client";
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
    }
}
