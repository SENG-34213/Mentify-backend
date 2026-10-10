package com.mentify.material.service;

import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.dto.response.S3UploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface MaterialStorageService {

    S3UploadResult upload(MultipartFile file, ValidatedMaterialFile validatedFile);

    void delete(String storageKey);
}
