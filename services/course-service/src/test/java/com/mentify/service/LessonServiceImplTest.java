package com.mentify.service;

import com.mentify.dto.LessonCreateRequest;
import com.mentify.dto.LessonResponse;
import com.mentify.entity.Course;
import com.mentify.entity.Lesson;
import com.mentify.entity.Module;
import com.mentify.enums.CourseStatus;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.LessonRepository;
import com.mentify.repository.ModuleRepository;
import com.mentify.service.impl.LessonServiceImpl;
import com.mentify.service.impl.TeacherCourseAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private TeacherCourseAccessGuard teacherCourseAccessGuard;

    private LessonServiceImpl lessonService;

    @BeforeEach
    void setUp() {
        lessonService = new LessonServiceImpl(moduleRepository, lessonRepository, teacherCourseAccessGuard);
    }

    @Test
    void createLesson_whenValidRequest_returnsCreated() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        Module module = baseModule(courseId, moduleId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson lesson = invocation.getArgument(0);
            lesson.setId(UUID.randomUUID());
            return lesson;
        });

        ApiResponse<LessonResponse> response = lessonService.createLesson(courseId, moduleId, LessonCreateRequest.builder()
                .title("  Linear Equations  ")
                .description("  Solve equations  ")
                .build());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getTitle()).isEqualTo("Linear Equations");

        verify(teacherCourseAccessGuard).assertTeacherOwnsCourse(module.getCourse());
    }

    @Test
    void createLesson_whenModuleNotFound_throwsNotFound() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.createLesson(courseId, moduleId, LessonCreateRequest.builder()
                .title("Lesson")
                .build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Module not found with id: '" + moduleId + "'");
    }

            @Test
            void updateLesson_whenValidRequest_returnsOk() {
            UUID courseId = UUID.randomUUID();
            UUID moduleId = UUID.randomUUID();
            UUID lessonId = UUID.randomUUID();
            Module module = baseModule(courseId, moduleId);
            Lesson lesson = Lesson.builder()
                .title("Old Lesson")
                .isVisible(true)
                .module(module)
                .build();
            lesson.setId(lessonId);

            when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
            when(lessonRepository.findByIdAndModule_Id(lessonId, moduleId)).thenReturn(Optional.of(lesson));
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ApiResponse<LessonResponse> response = lessonService.updateLesson(
                courseId,
                moduleId,
                lessonId,
                LessonCreateRequest.builder()
                    .title("  Updated Lesson  ")
                    .description("  Updated Description  ")
                    .isVisible(false)
                    .build()
            );

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
            assertThat(response.getMessage()).isEqualTo("Lesson updated successfully");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getTitle()).isEqualTo("Updated Lesson");
            assertThat(response.getData().isVisible()).isFalse();

            verify(teacherCourseAccessGuard).assertTeacherOwnsCourse(module.getCourse());
            }

            @Test
            void updateLesson_whenLessonNotFound_throwsNotFound() {
            UUID courseId = UUID.randomUUID();
            UUID moduleId = UUID.randomUUID();
            UUID lessonId = UUID.randomUUID();
            Module module = baseModule(courseId, moduleId);

            when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
            when(lessonRepository.findByIdAndModule_Id(lessonId, moduleId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lessonService.updateLesson(
                courseId,
                moduleId,
                lessonId,
                LessonCreateRequest.builder().title("Updated Lesson").build()
            ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Lesson not found with id: '" + lessonId + "'");
            }

    @Test
    void deleteLesson_whenValidRequest_returnsOk() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        Module module = baseModule(courseId, moduleId);
        Lesson lesson = Lesson.builder()
                .title("Lesson")
                .module(module)
                .build();
        lesson.setId(lessonId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.findByIdAndModule_Id(lessonId, moduleId)).thenReturn(Optional.of(lesson));

        ApiResponse<Object> response = lessonService.deleteLesson(courseId, moduleId, lessonId);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Lesson deleted successfully");
        verify(teacherCourseAccessGuard).assertTeacherOwnsCourse(module.getCourse());
        verify(lessonRepository).delete(lesson);
    }

    @Test
    void deleteLesson_whenLessonNotFound_throwsNotFound() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        Module module = baseModule(courseId, moduleId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.findByIdAndModule_Id(lessonId, moduleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.deleteLesson(courseId, moduleId, lessonId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Lesson not found with id: '" + lessonId + "'");
    }

    @Test
    void getLesson_whenValidRequest_returnsOk() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        Module module = baseModule(courseId, moduleId);
        Lesson lesson = Lesson.builder().title("Lesson 1").module(module).isVisible(true).build();
        lesson.setId(lessonId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.findByIdAndModule_Id(lessonId, moduleId)).thenReturn(Optional.of(lesson));

        ApiResponse<LessonResponse> response = lessonService.getLesson(courseId, moduleId, lessonId);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Lesson fetched successfully");
        assertThat(response.getData().getId()).isEqualTo(lessonId);
    }

    @Test
    void getLessons_whenValidRequest_returnsOk() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        Module module = baseModule(courseId, moduleId);
        Lesson lesson = Lesson.builder().title("Lesson 1").module(module).isVisible(true).build();
        lesson.setId(UUID.randomUUID());

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.findAllByModule_Id(moduleId)).thenReturn(List.of(lesson));

        ApiResponse<List<LessonResponse>> response = lessonService.getLessons(courseId, moduleId);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Lessons fetched successfully");
        assertThat(response.getData()).hasSize(1);
    }

    private Module baseModule(UUID courseId, UUID moduleId) {
        Course course = Course.builder()
                .courseName("Mathematics")
                .courseDescription("Grade 10")
                .courseFeeMonthly(new BigDecimal("2500.00"))
                .subject("Mathematics")
                .assignedTeacherId(UUID.randomUUID())
                .gradeId(UUID.randomUUID())
                .status(CourseStatus.DRAFT)
                .build();
        course.setId(courseId);

        Module module = Module.builder()
                .title("Algebra")
                .sequenceOrder(1)
                .isVisible(true)
                .course(course)
                .build();
        module.setId(moduleId);
        return module;
    }
}
