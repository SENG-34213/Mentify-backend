package com.mentify.ai.client;

import com.mentify.ai.client.dto.MaterialDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import com.mentify.payload.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "material-service", contextId = "aiMaterialAdminClient",
        path = "/api/v1/materials")
public interface MaterialAdminClient {

    @GetMapping
    ApiResponse<Page<MaterialDetailsResponse>> listMaterials(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
