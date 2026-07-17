package com.mentify.client;

import com.mentify.client.dto.CourseBulkLookupRequest;
import com.mentify.client.dto.CourseLookupResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "course-service", path = "/api/v1/course")
public interface CourseServiceClient {

    @PostMapping("/bulk")
    ApiResponse<List<CourseLookupResponse>> getCoursesByIds(
            @RequestBody CourseBulkLookupRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    );
}

