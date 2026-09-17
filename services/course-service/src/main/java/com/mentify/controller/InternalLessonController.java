package com.mentify.controller;

import com.mentify.dto.InternalLessonResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.InternalLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/lessons")
@RequiredArgsConstructor
public class InternalLessonController {

    private final InternalLessonService internalLessonService;

    @GetMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<InternalLessonResponse>> getLessonForAi(@PathVariable UUID lessonId) {
        ApiResponse<InternalLessonResponse> response = internalLessonService.getLessonForAi(lessonId);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
