package com.mentify.service;

import com.mentify.dto.ModuleCreateRequest;
import com.mentify.dto.ModuleResponse;
import com.mentify.entity.Course;
import com.mentify.entity.Module;
import com.mentify.enums.CourseStatus;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.repository.ModuleRepository;
import com.mentify.service.impl.ModuleServiceImpl;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private TeacherCourseAccessGuard teacherCourseAccessGuard;

    private ModuleServiceImpl moduleService;

    @BeforeEach
    void setUp() {
        moduleService = new ModuleServiceImpl(courseRepository, moduleRepository, teacherCourseAccessGuard);
    }

    @Test
    void createModule_whenValidRequest_returnsCreated() {
        UUID courseId = UUID.randomUUID();
        Course course = baseCourse(courseId);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> {
            Module module = invocation.getArgument(0);
            module.setId(UUID.randomUUID());
            return module;
        });

        ApiResponse<ModuleResponse> response = moduleService.createModule(courseId, ModuleCreateRequest.builder()
                .title("  Algebra  ")
                .description("  Basics  ")
                .sequenceOrder(1)
                .build());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getMessage()).isEqualTo("Module created successfully");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getTitle()).isEqualTo("Algebra");

        verify(teacherCourseAccessGuard).assertTeacherOwnsCourse(course);
    }

    @Test
    void createModule_whenCourseNotFound_throwsNotFound() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.createModule(courseId, ModuleCreateRequest.builder()
                .title("Algebra")
                .sequenceOrder(1)
                .build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Course not found with id: '" + courseId + "'");
    }

            @Test
            void updateModule_whenValidRequest_returnsOk() {
            UUID courseId = UUID.randomUUID();
            UUID moduleId = UUID.randomUUID();
            Course course = baseCourse(courseId);
            Module module = Module.builder()
                .title("Old Algebra")
                .sequenceOrder(1)
                .isVisible(true)
                .course(course)
                .build();
            module.setId(moduleId);

            when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
            when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ApiResponse<ModuleResponse> response = moduleService.updateModule(courseId, moduleId, ModuleCreateRequest.builder()
                .title("  Updated Algebra  ")
                .description("  Updated basics  ")
                .sequenceOrder(2)
                .isVisible(false)
                .build());

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
            assertThat(response.getMessage()).isEqualTo("Module updated successfully");
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().getTitle()).isEqualTo("Updated Algebra");
            assertThat(response.getData().getSequenceOrder()).isEqualTo(2);
            assertThat(response.getData().isVisible()).isFalse();

            verify(teacherCourseAccessGuard).assertTeacherOwnsCourse(course);
            }

            @Test
            void updateModule_whenModuleNotFound_throwsNotFound() {
            UUID courseId = UUID.randomUUID();
            UUID moduleId = UUID.randomUUID();

            when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> moduleService.updateModule(courseId, moduleId, ModuleCreateRequest.builder()
                .title("Updated Algebra")
                .sequenceOrder(1)
                .build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Module not found with id: '" + moduleId + "'");
            }

    private Course baseCourse(UUID courseId) {
        Course course = Course.builder()
                .courseName("Mathematics")
                .courseDescription("Grade 10 mathematics")
                .courseFeeMonthly(new BigDecimal("2500.00"))
                .subject("Mathematics")
                .assignedTeacherId(UUID.randomUUID())
                .gradeId(UUID.randomUUID())
                .status(CourseStatus.DRAFT)
                .build();
        course.setId(courseId);
        return course;
    }
}
