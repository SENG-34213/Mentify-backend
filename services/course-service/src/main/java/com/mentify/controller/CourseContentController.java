package com.mentify.controller;

import com.mentify.dto.LearningMaterialCreateRequest;
import com.mentify.dto.LearningMaterialResponse;
import com.mentify.dto.LessonCreateRequest;
import com.mentify.dto.LessonResponse;
import com.mentify.dto.ModuleCreateRequest;
import com.mentify.dto.ModuleResponse;
import com.mentify.payload.response.ApiResponse;
import com.mentify.service.LearningMaterialService;
import com.mentify.service.LessonService;
import com.mentify.service.ModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/course/{courseId}/modules")
@RequiredArgsConstructor
public class CourseContentController {

    private final ModuleService moduleService;
    private final LessonService lessonService;
    private final LearningMaterialService learningMaterialService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ModuleResponse>> createModule(
            @PathVariable UUID courseId,
            @Valid @RequestBody ModuleCreateRequest request
    ) {
        ApiResponse<ModuleResponse> response = moduleService.createModule(courseId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{moduleId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ModuleResponse>> updateModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @Valid @RequestBody ModuleCreateRequest request
    ) {
        ApiResponse<ModuleResponse> response = moduleService.updateModule(courseId, moduleId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/{moduleId}/lessons")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @Valid @RequestBody LessonCreateRequest request
    ) {
        ApiResponse<LessonResponse> response = lessonService.createLesson(courseId, moduleId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{moduleId}/lessons/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody LessonCreateRequest request
    ) {
        ApiResponse<LessonResponse> response = lessonService.updateLesson(courseId, moduleId, lessonId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/{moduleId}/learning-materials")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<LearningMaterialResponse>> createLearningMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @Valid @RequestBody LearningMaterialCreateRequest request
    ) {
        ApiResponse<LearningMaterialResponse> response =
            learningMaterialService.createLearningMaterial(courseId, moduleId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{moduleId}/learning-materials/{materialId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<LearningMaterialResponse>> updateLearningMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID materialId,
            @Valid @RequestBody LearningMaterialCreateRequest request
    ) {
        ApiResponse<LearningMaterialResponse> response =
                learningMaterialService.updateLearningMaterial(courseId, moduleId, materialId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
