package com.mentify.service.impl;

import com.mentify.dto.LearningMaterialCreateRequest;
import com.mentify.dto.LearningMaterialResponse;
import com.mentify.entity.LearningMaterial;
import com.mentify.entity.Lesson;
import com.mentify.entity.Module;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.LearningMaterialRepository;
import com.mentify.repository.LessonRepository;
import com.mentify.repository.ModuleRepository;
import com.mentify.service.LearningMaterialService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningMaterialServiceImpl implements LearningMaterialService {

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final LearningMaterialRepository learningMaterialRepository;
    private final TeacherCourseAccessGuard teacherCourseAccessGuard;

    @Override
    @Transactional
    public ApiResponse<LearningMaterialResponse> createLearningMaterial(
            UUID courseId,
            UUID moduleId,
            LearningMaterialCreateRequest request
    ) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());

        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findByIdAndModule_Id(request.getLessonId(), moduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        }

        LearningMaterial material = LearningMaterial.builder()
                .title(request.getTitle().trim())
                .type(request.getType())
                .fileUrl(request.getFileUrl().trim())
                .module(module)
                .lesson(lesson)
                .build();

        LearningMaterial savedMaterial = learningMaterialRepository.save(material);

        return ApiResponse.<LearningMaterialResponse>builder()
                .message("Learning material created successfully")
                .data(toResponse(savedMaterial))
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<LearningMaterialResponse> updateLearningMaterial(
            UUID courseId,
            UUID moduleId,
            UUID materialId,
            LearningMaterialCreateRequest request
    ) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());

        LearningMaterial material = learningMaterialRepository.findByIdAndModule_Id(materialId, moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("LearningMaterial", "id", materialId));

        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findByIdAndModule_Id(request.getLessonId(), moduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        }

        material.setTitle(request.getTitle().trim());
        material.setType(request.getType());
        material.setFileUrl(request.getFileUrl().trim());
        material.setLesson(lesson);

        LearningMaterial savedMaterial = learningMaterialRepository.save(material);

        return ApiResponse.<LearningMaterialResponse>builder()
                .message("Learning material updated successfully")
                .data(toResponse(savedMaterial))
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    private LearningMaterialResponse toResponse(LearningMaterial material) {
        return LearningMaterialResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .type(material.getType())
                .fileUrl(material.getFileUrl())
                .moduleId(material.getModule().getId())
                .lessonId(material.getLesson() != null ? material.getLesson().getId() : null)
                .createdBy(material.getCreatedBy())
                .updatedBy(material.getUpdatedBy())
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
                .build();
    }
}
