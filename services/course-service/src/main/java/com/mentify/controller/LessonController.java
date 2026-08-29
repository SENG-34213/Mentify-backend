package com.mentify.controller;

import com.mentify.dto.LessonRequest;
import com.mentify.dto.LessonResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/course/{courseId}/modules/{moduleId}/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @Valid @RequestBody LessonRequest request
    ) {
        ApiResponse<LessonResponse> response = lessonService.createLesson(courseId, moduleId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody LessonRequest request
    ) {
        ApiResponse<LessonResponse> response = lessonService.updateLesson(courseId, moduleId, lessonId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Object>> deleteLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId
    ) {
        ApiResponse<Object> response = lessonService.deleteLesson(courseId, moduleId, lessonId);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
