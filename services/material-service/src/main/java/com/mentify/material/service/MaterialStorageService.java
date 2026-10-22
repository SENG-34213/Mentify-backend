package com.mentify.material.service;

import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.dto.response.S3DownloadResult;
import com.mentify.material.dto.response.S3UploadResult;
import com.mentify.material.dto.response.S3FileResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MaterialStorageService {

    S3UploadResult upload(MultipartFile file, ValidatedMaterialFile validatedFile);

    default S3UploadResult upload(MultipartFile file, ValidatedMaterialFile validatedFile, String keyPrefix) {
        return upload(file, validatedFile);
    }

    default S3FileResult download(String storageKey) {
        throw new UnsupportedOperationException("Download is not supported by this storage adapter");
    }

    S3DownloadResult createPresignedDownload(String storageKey, String originalFilename, String contentType);

    boolean exists(String storageKey);

    List<String> listStorageKeys();

    void delete(String storageKey);
}
