package com.mentify.ai.client;

import com.mentify.ai.dto.tool.StudentAssignmentPage;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "assignment-service", contextId = "aiStudentAssignmentClient",
        path = "/api/v1/assignments")
public interface StudentAssignmentClient {

    @GetMapping("/me")
    ApiResponse<StudentAssignmentPage> getMyAssignments(
            @RequestParam UUID courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestHeader("Authorization") String authorizationHeader);
}
