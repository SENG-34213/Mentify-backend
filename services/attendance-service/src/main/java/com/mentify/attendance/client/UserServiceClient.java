package com.mentify.attendance.client;

import com.mentify.attendance.client.dto.StudentLookupRequest;
import com.mentify.attendance.client.dto.StudentLookupResponse;
import com.mentify.attendance.client.dto.TeacherLookupResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    @GetMapping("/teachers/{teacherId}/lookup")
    ApiResponse<TeacherLookupResponse> lookupTeacher(
            @PathVariable("teacherId") String teacherId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @PostMapping("/students/lookup")
    ApiResponse<List<StudentLookupResponse>> lookupStudents(
            @RequestBody StudentLookupRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
