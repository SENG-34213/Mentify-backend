package com.mentify.service.impl;

import com.mentify.dto.ModuleCreateRequest;
import com.mentify.dto.ModuleResponse;
import com.mentify.entity.Course;
import com.mentify.entity.Module;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.repository.LearningMaterialRepository;
import com.mentify.repository.LessonRepository;
import com.mentify.repository.ModuleRepository;
import com.mentify.service.ModuleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final LearningMaterialRepository learningMaterialRepository;
    private final TeacherCourseAccessGuard teacherCourseAccessGuard;

    @Override
    @Transactional
    public ApiResponse<ModuleResponse> createModule(UUID courseId, ModuleCreateRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(course);

        Module module = Module.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .moduleImage(request.getModuleImage())
                .sequenceOrder(request.getSequenceOrder())
                .dateDuration(request.getDateDuration())
                .isVisible(request.getIsVisible() != null ? request.getIsVisible() : true)
                .course(course)
                .build();

        Module savedModule = moduleRepository.save(module);

        return ApiResponse.<ModuleResponse>builder()
                .message("Module created successfully")
                .data(toResponse(savedModule))
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<ModuleResponse> updateModule(UUID courseId, UUID moduleId, ModuleCreateRequest request) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());

        module.setTitle(request.getTitle().trim());
        module.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        module.setModuleImage(request.getModuleImage());
        module.setSequenceOrder(request.getSequenceOrder());
        module.setDateDuration(request.getDateDuration());
        module.setVisible(request.getIsVisible() != null ? request.getIsVisible() : true);

        Module savedModule = moduleRepository.save(module);

        return ApiResponse.<ModuleResponse>builder()
                .message("Module updated successfully")
                .data(toResponse(savedModule))
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<ModuleResponse> getModule(UUID courseId, UUID moduleId) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());

        return ApiResponse.<ModuleResponse>builder()
                .message("Module fetched successfully")
                .data(toResponse(module))
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<List<ModuleResponse>> getModules(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(course);

        List<ModuleResponse> modules = moduleRepository.findAllByCourse_Id(courseId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.<List<ModuleResponse>>builder()
                .message("Modules fetched successfully")
                .data(modules)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<Object> deleteModule(UUID courseId, UUID moduleId) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());

        if (lessonRepository.existsByModule_Id(moduleId) || learningMaterialRepository.existsByModule_Id(moduleId)) {
            throw new IllegalArgumentException("Cannot delete module with existing lessons or learning materials");
        }

        moduleRepository.delete(module);

        return ApiResponse.builder()
                .message("Module deleted successfully")
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    private ModuleResponse toResponse(Module module) {
        return ModuleResponse.builder()
                .id(module.getId())
                .title(module.getTitle())
                .sequenceOrder(module.getSequenceOrder())
                .description(module.getDescription())
                .courseId(module.getCourse().getId())
                .dateDuration(module.getDateDuration())
                .isVisible(module.isVisible())
                .moduleImage(module.getModuleImage())
                .createdBy(module.getCreatedBy())
                .updatedBy(module.getUpdatedBy())
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
                .build();
    }
}
