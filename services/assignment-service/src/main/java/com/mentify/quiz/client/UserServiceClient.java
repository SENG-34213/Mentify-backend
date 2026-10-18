package com.mentify.quiz.client;

import com.mentify.payload.response.ApiResponse;
import com.mentify.quiz.client.dto.StudentLookupRequest;
import com.mentify.quiz.client.dto.StudentLookupResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    @GetMapping("/students/{studentId}/exists")
    boolean isStudentExists(
            @PathVariable UUID studentId,
            @RequestHeader("Authorization") String authorizationHeader
    );

    @PostMapping("/students/lookup")
    ApiResponse<List<StudentLookupResponse>> lookupStudents(
            @RequestBody StudentLookupRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
