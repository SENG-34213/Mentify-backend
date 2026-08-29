package com.mentify.service.impl;

import com.mentify.dto.LessonCreateRequest;
import com.mentify.dto.LessonResponse;
import com.mentify.entity.Lesson;
import com.mentify.entity.Module;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.LessonRepository;
import com.mentify.repository.ModuleRepository;
import com.mentify.service.LessonService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final TeacherCourseAccessGuard teacherCourseAccessGuard;

    @Override
    @Transactional
    public ApiResponse<LessonResponse> createLesson(UUID courseId, UUID moduleId, LessonCreateRequest request) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());

        Lesson lesson = Lesson.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .isVisible(request.getIsVisible() != null ? request.getIsVisible() : true)
                .releaseDate(request.getReleaseDate())
                .module(module)
                .build();

        Lesson savedLesson = lessonRepository.save(lesson);

        return ApiResponse.<LessonResponse>builder()
                .message("Lesson created successfully")
                .data(toResponse(savedLesson))
                .statusCode(HttpStatus.CREATED.value())
                .status(HttpStatus.CREATED)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<LessonResponse> updateLesson(
            UUID courseId,
            UUID moduleId,
            UUID lessonId,
            LessonCreateRequest request
    ) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());

        Lesson lesson = lessonRepository.findByIdAndModule_Id(lessonId, moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        lesson.setTitle(request.getTitle().trim());
        lesson.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        lesson.setVisible(request.getIsVisible() != null ? request.getIsVisible() : true);
        lesson.setReleaseDate(request.getReleaseDate());

        Lesson savedLesson = lessonRepository.save(lesson);

        return ApiResponse.<LessonResponse>builder()
                .message("Lesson updated successfully")
                .data(toResponse(savedLesson))
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<Object> deleteLesson(UUID courseId, UUID moduleId, UUID lessonId) {
        Module module = moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", moduleId));

        teacherCourseAccessGuard.assertTeacherOwnsCourse(module.getCourse());

        Lesson lesson = lessonRepository.findByIdAndModule_Id(lessonId, moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        lessonRepository.delete(lesson);

        return ApiResponse.builder()
                .message("Lesson deleted successfully")
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }

    private LessonResponse toResponse(Lesson lesson) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .isVisible(lesson.isVisible())
                .releaseDate(lesson.getReleaseDate())
                .moduleId(lesson.getModule().getId())
                .createdBy(lesson.getCreatedBy())
                .updatedBy(lesson.getUpdatedBy())
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }
}
