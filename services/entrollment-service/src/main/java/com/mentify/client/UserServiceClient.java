package com.mentify.client;

import com.mentify.client.dto.UserLookupResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    @GetMapping
    ApiResponse<List<UserLookupResponse>> getUsersByRole(
            @RequestParam("role") String role,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
