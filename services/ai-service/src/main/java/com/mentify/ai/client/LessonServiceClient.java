package com.mentify.ai.client;

import com.mentify.ai.dto.response.InternalLessonResponse;
import com.mentify.payload.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "course-service", path = "/api/v1/internal/lessons")
public interface LessonServiceClient {

    @GetMapping("/{lessonId}")
    ApiResponse<InternalLessonResponse> getLessonForAi(
            @PathVariable("lessonId") UUID lessonId,
            @RequestHeader("Authorization") String authorizationHeader
    );
}
