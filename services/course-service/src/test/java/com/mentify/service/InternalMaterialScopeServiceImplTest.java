package com.mentify.service;

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
import com.mentify.service.impl.InternalMaterialScopeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalMaterialScopeServiceImplTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LearningMaterialRepository learningMaterialRepository;

    @Mock
    private com.mentify.service.impl.CourseContentAccessGuard courseContentAccessGuard;

    private InternalMaterialScopeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InternalMaterialScopeServiceImpl(
                courseRepository,
                moduleRepository,
                lessonRepository,
                learningMaterialRepository,
                courseContentAccessGuard
        );
    }

    @Test
    void courseScopeReturnsMaterialIdsOwnedByCourseServiceRelationships() {
        UUID courseId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        Course course = Course.builder().build();
        course.setId(courseId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(learningMaterialRepository.findMaterialIdsByCourseId(courseId)).thenReturn(List.of(materialId));

        ApiResponse<MaterialScopeResponse> response = service.getCourseMaterialScope(courseId, AUTH_HEADER);

        assertThat(response.getData().getCourseId()).isEqualTo(courseId);
        assertThat(response.getData().getMaterialIds()).containsExactly(materialId);
        verify(courseContentAccessGuard).assertCanReadCourseContent(course, AUTH_HEADER);
    }

    @Test
    void moduleScopeReturnsOwningCourseAndModuleMaterialIds() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        Course course = Course.builder().build();
        course.setId(courseId);
        Module module = Module.builder().course(course).build();
        module.setId(moduleId);
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(module));
        when(learningMaterialRepository.findMaterialIdsByModuleId(moduleId)).thenReturn(List.of(materialId));

        ApiResponse<MaterialScopeResponse> response = service.getModuleMaterialScope(moduleId, AUTH_HEADER);

        assertThat(response.getData().getCourseId()).isEqualTo(courseId);
        assertThat(response.getData().getMaterialIds()).containsExactly(materialId);
    }

    @Test
    void lessonScopeReturnsOwningCourseAndLessonMaterialIds() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        Course course = Course.builder().build();
        course.setId(courseId);
        Module module = Module.builder().course(course).build();
        module.setId(moduleId);
        Lesson lesson = Lesson.builder().module(module).build();
        lesson.setId(lessonId);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(learningMaterialRepository.findMaterialIdsByLessonId(lessonId)).thenReturn(List.of(materialId));

        ApiResponse<MaterialScopeResponse> response = service.getLessonMaterialScope(lessonId, AUTH_HEADER);

        assertThat(response.getData().getCourseId()).isEqualTo(courseId);
        assertThat(response.getData().getMaterialIds()).containsExactly(materialId);
        verify(learningMaterialRepository).findMaterialIdsByLessonId(lessonId);
    }

    @Test
    void missingModuleReturnsNotFound() {
        UUID moduleId = UUID.randomUUID();
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getModuleMaterialScope(moduleId, AUTH_HEADER))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
