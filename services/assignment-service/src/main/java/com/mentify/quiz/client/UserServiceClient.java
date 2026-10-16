package com.mentify.quiz.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "user-service", path = "/api/v1/users")
public interface UserServiceClient {

    @GetMapping("/students/{studentId}/exists")
    boolean isStudentExists(
            @PathVariable UUID studentId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
