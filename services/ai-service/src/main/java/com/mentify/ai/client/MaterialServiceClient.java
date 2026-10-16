package com.mentify.ai.client;

import com.mentify.ai.client.dto.MaterialDetailsResponse;
import com.mentify.ai.client.dto.MaterialDownloadResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "material-service", path = "/api/v1/materials")
public interface MaterialServiceClient {

    @GetMapping("/{materialId}")
    ApiResponse<MaterialDetailsResponse> getMaterial(
            @PathVariable UUID materialId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/{materialId}/download")
    ApiResponse<MaterialDownloadResponse> createDownload(
            @PathVariable UUID materialId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
