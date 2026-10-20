package com.mentify.communication.client;

import com.mentify.communication.client.dto.MaterialDownloadResponse;
import com.mentify.communication.client.dto.MaterialLookupResponse;
import com.mentify.communication.client.dto.MaterialUploadResponse;
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
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("purpose") String purpose,
            @RequestParam("courseId") UUID courseId,
            @RequestPart("file") MultipartFile file,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/{materialId}")
    ApiResponse<MaterialLookupResponse> getMaterial(
            @PathVariable UUID materialId,
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
