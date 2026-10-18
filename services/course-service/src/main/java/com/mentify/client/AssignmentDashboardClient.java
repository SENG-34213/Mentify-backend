package com.mentify.client;

import com.mentify.client.dto.AssignmentDashboardResponse;
import com.mentify.client.dto.SubmissionDashboardResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "assignment-service", path = "/api/v1")
public interface AssignmentDashboardClient {

    @GetMapping("/courses/{courseId}/assignments")
    ApiResponse<List<AssignmentDashboardResponse>> getAssignments(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/assignments/{assignmentId}/submissions")
    ApiResponse<List<SubmissionDashboardResponse>> getSubmissions(
            @PathVariable UUID assignmentId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
