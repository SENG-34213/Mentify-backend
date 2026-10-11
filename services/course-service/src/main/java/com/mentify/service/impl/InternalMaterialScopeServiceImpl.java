package com.mentify.service.impl;

import com.mentify.dto.MaterialScopeResponse;
import com.mentify.entity.Course;
import com.mentify.entity.Lesson;
import com.mentify.entity.Module;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.repository.LearningMaterialRepository;
import com.mentify.repository.LessonRepository;
import com.mentify.repository.ModuleRepository;
import com.mentify.service.InternalMaterialScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalMaterialScopeServiceImpl implements InternalMaterialScopeService {

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final LearningMaterialRepository learningMaterialRepository;
    private final CourseContentAccessGuard courseContentAccessGuard;

    @Override
    public ApiResponse<MaterialScopeResponse> getCourseMaterialScope(UUID courseId, String authorizationHeader) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        courseContentAccessGuard.assertCanReadCourseContent(course, authorizationHeader);
        return scopeResponse(course.getId(), learningMaterialRepository.findMaterialIdsByCourseId(course.getId()));
    }

    @Override
    public ApiResponse<MaterialScopeResponse> getModuleMaterialScope(UUID moduleId, String authorizationHeader) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));
        courseContentAccessGuard.assertCanReadCourseContent(module.getCourse(), authorizationHeader);
        return scopeResponse(module.getCourse().getId(), learningMaterialRepository.findMaterialIdsByModuleId(module.getId()));
    }

    @Override
    public ApiResponse<MaterialScopeResponse> getLessonMaterialScope(UUID lessonId, String authorizationHeader) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));
        courseContentAccessGuard.assertCanReadCourseContent(lesson.getModule().getCourse(), authorizationHeader);
        return scopeResponse(
                lesson.getModule().getCourse().getId(),
                learningMaterialRepository.findMaterialIdsByLessonId(lesson.getId())
        );
    }

    private ApiResponse<MaterialScopeResponse> scopeResponse(UUID courseId, List<UUID> materialIds) {
        return ApiResponse.<MaterialScopeResponse>builder()
                .message("Material scope fetched successfully")
                .data(MaterialScopeResponse.builder()
                        .courseId(courseId)
                        .materialIds(materialIds)
                        .build())
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }
}
