package com.mentify.material.controller;

import com.mentify.material.dto.ValidatedMaterialFile;
import com.mentify.material.dto.response.ProfileImageUploadResponse;
import com.mentify.material.dto.response.S3FileResult;
import com.mentify.material.dto.response.S3UploadResult;
import com.mentify.material.security.CurrentMaterialUserService;
import com.mentify.material.service.MaterialFileValidationService;
import com.mentify.material.service.MaterialStorageService;
import com.mentify.material.exception.MaterialValidationException;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/profile-images")
public class ProfileImageController {

    private final MaterialFileValidationService fileValidationService;
    private final MaterialStorageService storageService;
    private final CurrentMaterialUserService currentUserService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ProfileImageUploadResponse>> upload(@RequestParam MultipartFile file) {
        ValidatedMaterialFile validated = fileValidationService.validate(file);
        if (!validated.contentType().startsWith("image/")) {
            throw new MaterialValidationException("Profile image must be a valid image file");
        }
        if (validated.fileSize() > 5 * 1024 * 1024) {
            throw new MaterialValidationException("Profile image must be smaller than 5 MB");
        }

        UUID userId = currentUserService.getCurrentUserId();
        S3UploadResult uploaded = storageService.upload(file, validated, "profile-images/" + userId);
        ProfileImageUploadResponse response = new ProfileImageUploadResponse(uploaded.storageKey(), validated.contentType());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Profile image uploaded successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> download(@RequestParam String key) {
        var currentUser = currentUserService.getCurrentUser();
        String ownPrefix = "materials/profile-images/" + currentUser.userId() + "/";
        boolean canReadOwnImage = key.startsWith(ownPrefix);
        boolean canReadDirectoryImage = currentUser.isTeacher() || currentUser.isAdmin();
        if (!canReadOwnImage && !canReadDirectoryImage
                || !key.startsWith("materials/profile-images/")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        S3FileResult file = storageService.download(key);
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(file.contentType());
        } catch (Exception ignored) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(file.contentLength())
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(file.content());
    }
}
