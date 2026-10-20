package com.mentify.controller;

import com.mentify.common.notification.NotifyOnSuccess;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/course/{courseId}/modules")
@RequiredArgsConstructor
public class CourseContentController {

    private final ModuleService moduleService;
    private final LessonService lessonService;
    private final LearningMaterialService learningMaterialService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
//    @NotifyOnSuccess(type = "COURSE_MODULE_CREATED", title = "Course module created", message = "A course module was created successfully.")
    public ResponseEntity<ApiResponse<ModuleResponse>> createModule(
            @PathVariable UUID courseId,
            @Valid @RequestBody ModuleCreateRequest request
    ) {
        ApiResponse<ModuleResponse> response = moduleService.createModule(courseId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{moduleId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ModuleResponse>> updateModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @Valid @RequestBody ModuleCreateRequest request
    ) {
        ApiResponse<ModuleResponse> response = moduleService.updateModule(courseId, moduleId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getModules(@PathVariable UUID courseId) {
        ApiResponse<List<ModuleResponse>> response = moduleService.getModules(courseId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/{moduleId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ModuleResponse>> getModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId
    ) {
        ApiResponse<ModuleResponse> response = moduleService.getModule(courseId, moduleId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/{moduleId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId
    ) {
        ApiResponse<Object> response = moduleService.deleteModule(courseId, moduleId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/{moduleId}/lessons")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
//    @NotifyOnSuccess(type = "COURSE_LESSON_CREATED", title = "Lesson created", message = "A course lesson was created successfully.")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @Valid @RequestBody LessonCreateRequest request
    ) {
        ApiResponse<LessonResponse> response = lessonService.createLesson(courseId, moduleId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{moduleId}/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody LessonCreateRequest request
    ) {
        ApiResponse<LessonResponse> response = lessonService.updateLesson(courseId, moduleId, lessonId, request);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/{moduleId}/lessons")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getLessons(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId
    ) {
        ApiResponse<List<LessonResponse>> response = lessonService.getLessons(courseId, moduleId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @GetMapping("/{moduleId}/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> getLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId
    ) {
        ApiResponse<LessonResponse> response = lessonService.getLesson(courseId, moduleId, lessonId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @DeleteMapping("/{moduleId}/lessons/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId
    ) {
        ApiResponse<Object> response = lessonService.deleteLesson(courseId, moduleId, lessonId);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/{moduleId}/learning-materials")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
//    @NotifyOnSuccess(type = "COURSE_MATERIAL_CREATED", title = "Course material added", message = "Learning material was added to the course successfully.")
    public ResponseEntity<ApiResponse<LearningMaterialResponse>> createLearningMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @Valid @RequestBody LearningMaterialCreateRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<LearningMaterialResponse> response =
            learningMaterialService.createLearningMaterial(courseId, moduleId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/{moduleId}/learning-materials/upload")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LearningMaterialResponse>> uploadLearningMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID lessonId,
            @RequestParam MultipartFile file,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<LearningMaterialResponse> response = learningMaterialService.uploadLearningMaterial(
                courseId, moduleId, lessonId, title, description, file, authorizationHeader
        );
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/{moduleId}/lessons/{lessonId}/materials/upload")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LearningMaterialResponse>> uploadLessonMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile file,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<LearningMaterialResponse> response = learningMaterialService.uploadLearningMaterial(
                courseId, moduleId, lessonId, title, description, file, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PutMapping("/{moduleId}/learning-materials/{materialId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<LearningMaterialResponse>> updateLearningMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID materialId,
            @Valid @RequestBody LearningMaterialCreateRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<LearningMaterialResponse> response =
                learningMaterialService.updateLearningMaterial(courseId, moduleId, materialId, request, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

        @GetMapping("/{moduleId}/learning-materials")
        @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<List<LearningMaterialResponse>>> getLearningMaterials(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @RequestHeader("Authorization") String authorizationHeader
        ) {
        ApiResponse<List<LearningMaterialResponse>> response =
            learningMaterialService.getLearningMaterials(courseId, moduleId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
        }

    @GetMapping("/{moduleId}/lessons/{lessonId}/materials")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<LearningMaterialResponse>>> getLessonMaterials(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        ApiResponse<List<LearningMaterialResponse>> response =
                learningMaterialService.getLessonLearningMaterials(courseId, moduleId, lessonId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
    }

        @GetMapping("/{moduleId}/learning-materials/{materialId}")
        @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN')")
        public ResponseEntity<ApiResponse<LearningMaterialResponse>> getLearningMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID materialId,
            @RequestHeader("Authorization") String authorizationHeader
        ) {
        ApiResponse<LearningMaterialResponse> response =
            learningMaterialService.getLearningMaterial(courseId, moduleId, materialId, authorizationHeader);
        return new ResponseEntity<>(response, response.getStatus());
        }

    @DeleteMapping("/{moduleId}/learning-materials/{materialId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Object>> deleteLearningMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @PathVariable UUID materialId
    ) {
        ApiResponse<Object> response = learningMaterialService.deleteLearningMaterial(courseId, moduleId, materialId);
        return new ResponseEntity<>(response, response.getStatus());
    }
}
