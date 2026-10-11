package com.mentify.service.impl;

import com.mentify.client.MaterialServiceClient;
import com.mentify.client.dto.MaterialLookupResponse;
import com.mentify.dto.LearningMaterialCreateRequest;
import com.mentify.dto.LearningMaterialResponse;
import com.mentify.entity.LearningMaterial;
import com.mentify.entity.Lesson;
import com.mentify.entity.Module;
import com.mentify.enums.MaterialType;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.LearningMaterialRepository;
import com.mentify.repository.LessonRepository;
import com.mentify.repository.ModuleRepository;
import com.mentify.service.LearningMaterialService;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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
    private final CourseContentAccessGuard courseContentAccessGuard;
    private final MaterialServiceClient materialServiceClient;

    @Override
    @Transactional
    public ApiResponse<LearningMaterialResponse> createLearningMaterial(
            UUID courseId,
            UUID moduleId,
            LearningMaterialCreateRequest request,
            String authorizationHeader
    ) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());
        MaterialLookupResponse materialReference = validateMaterialReference(courseId, request.getMaterialId(), authorizationHeader);
        validateRequest(request, materialReference);

        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findByIdAndModule_Id(request.getLessonId(), moduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        }

        LearningMaterial material = LearningMaterial.builder()
                .title(request.getTitle().trim())
                .type(resolveType(request, materialReference))
                .fileUrl(resolveFileUrl(request, materialReference))
                .materialId(request.getMaterialId())
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
            LearningMaterialCreateRequest request,
            String authorizationHeader
    ) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());
        MaterialLookupResponse materialReference = validateMaterialReference(courseId, request.getMaterialId(), authorizationHeader);
        validateRequest(request, materialReference);

        LearningMaterial material = learningMaterialRepository.findByIdAndModule_Id(materialId, moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("LearningMaterial", "id", materialId));

        Lesson lesson = null;
        if (request.getLessonId() != null) {
            lesson = lessonRepository.findByIdAndModule_Id(request.getLessonId(), moduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", request.getLessonId()));
        }

        material.setTitle(request.getTitle().trim());
        material.setType(resolveType(request, materialReference));
        material.setFileUrl(resolveFileUrl(request, materialReference));
        material.setMaterialId(request.getMaterialId());
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
    public ApiResponse<LearningMaterialResponse> getLearningMaterial(
            UUID courseId,
            UUID moduleId,
            UUID materialId,
            String authorizationHeader
    ) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        courseContentAccessGuard.assertCanReadCourseContent(module.getCourse(), authorizationHeader);

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
    public ApiResponse<List<LearningMaterialResponse>> getLearningMaterials(
            UUID courseId,
            UUID moduleId,
            String authorizationHeader
    ) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        courseContentAccessGuard.assertCanReadCourseContent(module.getCourse(), authorizationHeader);

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
                .materialId(material.getMaterialId())
                .moduleId(material.getModule().getId())
                .lessonId(material.getLesson() != null ? material.getLesson().getId() : null)
                .createdBy(material.getCreatedBy())
                .updatedBy(material.getUpdatedBy())
                .createdAt(material.getCreatedAt())
                .updatedAt(material.getUpdatedAt())
                .build();
    }

    private void validateRequest(LearningMaterialCreateRequest request, MaterialLookupResponse materialReference) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Learning material title is required");
        }
        if (request.getTitle().trim().length() > 150) {
            throw new IllegalArgumentException("Learning material title must not exceed 150 characters");
        }
        if (materialReference == null && (request.getFileUrl() == null || request.getFileUrl().isBlank())) {
            throw new IllegalArgumentException("Either materialId or fileUrl is required");
        }
        if (materialReference == null && request.getType() == null) {
            throw new IllegalArgumentException("Learning material type is required for legacy fileUrl materials");
        }
    }

    private MaterialLookupResponse validateMaterialReference(
            UUID courseId,
            UUID materialId,
            String authorizationHeader
    ) {
        if (materialId == null) {
            return null;
        }

        MaterialLookupResponse material = loadMaterial(materialId, authorizationHeader);
        if (material.courseId() == null || !material.courseId().equals(courseId)) {
            throw new AccessDeniedException("Material does not belong to the target course");
        }
        return material;
    }

    private MaterialLookupResponse loadMaterial(UUID materialId, String authorizationHeader) {
        try {
            ApiResponse<MaterialLookupResponse> response = materialServiceClient.getMaterial(materialId, authorizationHeader);
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException("Material", "id", materialId);
            }
            return response.getData();
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Material", "id", materialId);
        } catch (FeignException.Forbidden | FeignException.Unauthorized ex) {
            throw new AccessDeniedException("Material access denied", ex);
        }
    }

    private MaterialType resolveType(LearningMaterialCreateRequest request, MaterialLookupResponse materialReference) {
        if (request.getType() != null) {
            return request.getType();
        }
        return mapMaterialType(materialReference.type());
    }

    private String resolveFileUrl(LearningMaterialCreateRequest request, MaterialLookupResponse materialReference) {
        if (materialReference != null) {
            return null;
        }
        return request.getFileUrl().trim();
    }

    private MaterialType mapMaterialType(String materialServiceType) {
        if (materialServiceType == null || materialServiceType.isBlank()) {
            throw new IllegalArgumentException("Material type is required");
        }
        return switch (materialServiceType) {
            case "PDF" -> MaterialType.PDF;
            case "VIDEO" -> MaterialType.VIDEO;
            case "IMAGE" -> MaterialType.IMAGE;
            case "AUDIO" -> MaterialType.AUDIO;
            case "DOCUMENT" -> MaterialType.DOC;
            case "PRESENTATION" -> MaterialType.PPT;
            default -> throw new IllegalArgumentException("Unsupported material type: " + materialServiceType);
        };
    }
}
