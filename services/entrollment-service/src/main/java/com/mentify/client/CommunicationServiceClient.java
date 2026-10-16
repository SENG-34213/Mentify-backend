package com.mentify.client;

import com.mentify.client.dto.AddStudentToGroupRequest;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "communication-service", path = "/api/communication/groups")
public interface CommunicationServiceClient {

    @PostMapping("/courses/{courseId}/students")
    ApiResponse<Object> addStudentToCourseGroup(
            @PathVariable UUID courseId,
            @RequestBody AddStudentToGroupRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
