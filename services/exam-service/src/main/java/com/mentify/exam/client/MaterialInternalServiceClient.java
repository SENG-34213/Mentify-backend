package com.mentify.exam.client;

import com.mentify.exam.client.dto.MaterialAssociationRequest;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "material-service", contextId = "examMaterialInternalServiceClient")
public interface MaterialInternalServiceClient {
    @PostMapping("/api/v1/internal/materials/{materialId}/association")
    ApiResponse<Object> associate(
            @PathVariable UUID materialId,
            @RequestBody MaterialAssociationRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
