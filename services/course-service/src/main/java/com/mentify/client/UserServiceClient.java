package com.mentify.client;

import com.mentify.client.dto.StudentDirectoryProfileResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    @GetMapping("/students/lookup")
    ApiResponse<List<StudentDirectoryProfileResponse>> getStudentsByIds(
            @RequestParam("ids") List<UUID> ids,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/teachers/{teacherId}/exists")
    boolean isTeacherExists(@PathVariable String teacherId, @RequestHeader("Authorization") String authorizationHeader);

    @GetMapping("/teachers/{teacherId}/canonical-id")
    UUID getTeacherCanonicalId(@PathVariable String teacherId, @RequestHeader("Authorization") String authorizationHeader);
}
