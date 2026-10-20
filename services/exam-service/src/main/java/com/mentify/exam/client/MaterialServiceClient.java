package com.mentify.exam.client;

import com.mentify.exam.client.dto.MaterialDownloadResponse;
import com.mentify.exam.client.dto.MaterialUploadResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@FeignClient(name = "material-service", path = "/api/v1/materials")
public interface MaterialServiceClient {

    @PostMapping(consumes = "multipart/form-data")
    ApiResponse<MaterialUploadResponse> upload(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String purpose,
            @RequestParam UUID courseId,
            @RequestPart MultipartFile file,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/{materialId}/download")
    ApiResponse<MaterialDownloadResponse> createDownload(
            @PathVariable UUID materialId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @DeleteMapping("/{materialId}")
    ApiResponse<Object> deleteMaterial(
            @PathVariable UUID materialId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
