package com.mentify.ai.client;

import com.mentify.ai.dto.tool.StudentAttendanceSummary;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "attendance-service", contextId = "aiStudentAttendanceClient",
        path = "/api/v1/attendance")
public interface StudentAttendanceClient {

    @GetMapping("/courses/{courseId}/me")
    ApiResponse<StudentAttendanceSummary> getMyAttendance(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader);
}
