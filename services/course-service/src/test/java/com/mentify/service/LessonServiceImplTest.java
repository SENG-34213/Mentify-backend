package com.mentify.service;

import com.mentify.dto.LessonRequest;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

        ApiResponse<LessonResponse> response = lessonService.createLesson(courseId, moduleId, validRequest("L1"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getMessage()).isEqualTo("Lesson created successfully");
    }

    @Test
    void updateLesson_whenValidRequest_returnsOk() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        Module module = baseModule(courseId, moduleId);
        Lesson lesson = Lesson.builder().title("Old").module(module).isVisible(true).build();
        lesson.setId(lessonId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.findByIdAndModule_Id(lessonId, moduleId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<LessonResponse> response = lessonService.updateLesson(courseId, moduleId, lessonId, validRequest("New"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Lesson updated successfully");
        assertThat(response.getData().getTitle()).isEqualTo("New");
    }

    @Test
    void deleteLesson_whenFound_returnsOkAndDeletes() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        Module module = baseModule(courseId, moduleId);
        Lesson lesson = Lesson.builder().title("Old").module(module).isVisible(true).build();
        lesson.setId(lessonId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.findByIdAndModule_Id(lessonId, moduleId)).thenReturn(Optional.of(lesson));

        ApiResponse<Object> response = lessonService.deleteLesson(courseId, moduleId, lessonId);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Lesson deleted successfully");
        verify(lessonRepository).delete(lesson);
    }

    @Test
    void deleteLesson_whenNotFound_throwsNotFound() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        Module module = baseModule(courseId, moduleId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.findByIdAndModule_Id(lessonId, moduleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.deleteLesson(courseId, moduleId, lessonId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Lesson not found with id: '" + lessonId + "'");

        verify(lessonRepository, never()).delete(any(Lesson.class));
    }

    private LessonRequest validRequest(String title) {
        return LessonRequest.builder().title(title).isVisible(true).build();
    }

    private Module baseModule(UUID courseId, UUID moduleId) {
        Course course = Course.builder()
                .courseName("Mathematics")
                .courseDescription("Grade 10")
                .courseFeeMonthly(new BigDecimal("1000.00"))
                .subject("Math")
                .gradeId(UUID.randomUUID())
                .assignedTeacherId(UUID.randomUUID())
                .status(CourseStatus.DRAFT)
                .build();
        course.setId(courseId);

        Module module = Module.builder().title("Algebra").sequenceOrder(1).course(course).isVisible(true).build();
        module.setId(moduleId);
        return module;
    }
}
