package com.mentify.controller;

import com.mentify.dto.CourseProgressResponse;
import com.mentify.dto.LessonProgressResponse;
import com.mentify.dto.LessonProgressUpdateRequest;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.StudentProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentProgressController {

    private final StudentProgressService studentProgressService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<LessonProgressResponse>>> getMyProgress() {
        return ResponseEntity.ok(ApiResponse.success(200, "Student progress fetched successfully",
                studentProgressService.getMyProgress()));
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<ApiResponse<CourseProgressResponse>> getCourseProgress(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(ApiResponse.success(200, "Course progress fetched successfully",
                studentProgressService.getCourseProgress(courseId, authorizationHeader)));
    }

    @PutMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<LessonProgressResponse>> updateLessonProgress(
            @PathVariable UUID lessonId,
            @Valid @RequestBody LessonProgressUpdateRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(ApiResponse.success(200, "Lesson progress updated successfully",
                studentProgressService.updateLessonProgress(lessonId, request, authorizationHeader)));
    }
}
