package com.mentify.service;

import com.mentify.dto.ModuleRequest;
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
import static org.mockito.Mockito.never;
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

        ApiResponse<ModuleResponse> response = moduleService.createModule(courseId, validRequest("Algebra", 1));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getMessage()).isEqualTo("Module created successfully");
        verify(teacherCourseAccessGuard).assertTeacherOwnsCourse(course);
    }

    @Test
    void updateModule_whenValidRequest_returnsOk() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        Course course = baseCourse(courseId);
        Module module = Module.builder().title("Old").sequenceOrder(1).course(course).isVisible(true).build();
        module.setId(moduleId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<ModuleResponse> response = moduleService.updateModule(courseId, moduleId, validRequest("New", 2));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Module updated successfully");
        assertThat(response.getData().getTitle()).isEqualTo("New");
    }

    @Test
    void deleteModule_whenFound_returnsOkAndDeletes() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        Course course = baseCourse(courseId);
        Module module = Module.builder().title("Old").sequenceOrder(1).course(course).isVisible(true).build();
        module.setId(moduleId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));

        ApiResponse<Object> response = moduleService.deleteModule(courseId, moduleId);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Module deleted successfully");
        verify(moduleRepository).delete(module);
    }

    @Test
    void deleteModule_whenNotFound_throwsNotFound() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.deleteModule(courseId, moduleId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Module not found with id: '" + moduleId + "'");

        verify(moduleRepository, never()).delete(any(Module.class));
    }

    private ModuleRequest validRequest(String title, int sequenceOrder) {
        return ModuleRequest.builder().title(title).sequenceOrder(sequenceOrder).isVisible(true).build();
    }

    private Course baseCourse(UUID courseId) {
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
        return course;
    }
}
