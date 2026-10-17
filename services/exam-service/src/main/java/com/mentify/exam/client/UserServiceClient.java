package com.mentify.exam.client;

import com.mentify.exam.client.dto.StudentLookupResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    @GetMapping("/students/lookup")
    ApiResponse<List<StudentLookupResponse>> lookupStudents(
            @RequestParam("ids") List<UUID> ids,
            @RequestHeader("Authorization") String authorizationHeader);
}
