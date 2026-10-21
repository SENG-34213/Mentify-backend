package com.mentify.calendar.client;

import com.mentify.calendar.client.dto.TeacherLookupResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${mentify.clients.user-service.url:}", path = "/api/v1/users")
public interface UserServiceClient {

    @GetMapping("/teachers/{teacherId}/lookup")
    ApiResponse<TeacherLookupResponse> getTeacherById(
            @PathVariable("teacherId") UUID teacherId,
            @RequestHeader("Authorization") String authorizationHeader);
}
