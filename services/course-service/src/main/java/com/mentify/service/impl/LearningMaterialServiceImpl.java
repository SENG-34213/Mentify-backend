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

import java.util.List;
import java.util.UUID;

@Service
// TODO: Replace the current local file-url handling with S3 bucket storage later when the cloud setup is ready.
// Cloud push command for later: aws s3 cp ./uploads/course-materials s3://<your-bucket-name>/course-materials --recursive
// This is kept local for now; same TODO was added in the assignment service storage class.
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

    @Override
    @Transactional
    public ApiResponse<LearningMaterialResponse> getLearningMaterial(UUID courseId, UUID moduleId, UUID materialId) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());

        LearningMaterial material = learningMaterialRepository.findByIdAndModule_Id(materialId, moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("LearningMaterial", "id", materialId));

        return ApiResponse.<LearningMaterialResponse>builder()
                .message("Learning material fetched successfully")
                .data(toResponse(material))
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<List<LearningMaterialResponse>> getLearningMaterials(UUID courseId, UUID moduleId) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());

        List<LearningMaterialResponse> materials = learningMaterialRepository.findAllByModule_Id(moduleId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.<List<LearningMaterialResponse>>builder()
                .message("Learning materials fetched successfully")
                .data(materials)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<Object> deleteLearningMaterial(UUID courseId, UUID moduleId, UUID materialId) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());

        LearningMaterial material = learningMaterialRepository.findByIdAndModule_Id(materialId, moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("LearningMaterial", "id", materialId));

        learningMaterialRepository.delete(material);

        return ApiResponse.builder()
                .message("Learning material deleted successfully")
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
