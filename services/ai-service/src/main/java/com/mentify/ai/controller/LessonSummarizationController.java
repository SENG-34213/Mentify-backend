package com.mentify.ai.controller;

import com.mentify.ai.dto.response.LessonSummaryResponse;
import com.mentify.ai.service.LessonSummarizationService;
import com.mentify.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/ai/lessons")
@RequiredArgsConstructor
public class LessonSummarizationController {

    private final LessonSummarizationService lessonSummarizationService;

    @PostMapping("/{lessonId}/summarize")
    public ResponseEntity<ApiResponse<LessonSummaryResponse>> summarizeLesson(
            @PathVariable UUID lessonId,
            @RequestHeader("Authorization") String authorizationHeader) {
        
        log.info("Received request to summarize lesson: {}", lessonId);
        
        LessonSummaryResponse response = lessonSummarizationService.summarizeLesson(lessonId, authorizationHeader);
        
        return ResponseEntity.ok(ApiResponse.<LessonSummaryResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Lesson summary generated successfully")
                .data(response)
                .build());
    }
}
