package com.mentify.material.service;

import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.dto.response.S3DownloadResult;
import com.mentify.material.dto.response.S3UploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface MaterialStorageService {

    S3UploadResult upload(MultipartFile file, ValidatedMaterialFile validatedFile);

    S3DownloadResult createPresignedDownload(String storageKey, String originalFilename, String contentType);

    void delete(String storageKey);
}
