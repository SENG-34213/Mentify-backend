package com.mentify.client;

import com.mentify.client.dto.MaterialLookupResponse;
import com.mentify.client.dto.MaterialUploadResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @org.springframework.web.bind.annotation.DeleteMapping("/{materialId}")
    ApiResponse<Object> deleteMaterial(
            @PathVariable UUID materialId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
