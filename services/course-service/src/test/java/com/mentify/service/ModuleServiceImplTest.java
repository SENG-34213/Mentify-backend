package com.mentify.service;

import com.mentify.dto.ModuleCreateRequest;
import com.mentify.dto.ModuleResponse;
import com.mentify.entity.Course;
import com.mentify.entity.Module;
import com.mentify.enums.CourseStatus;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.repository.LearningMaterialRepository;
import com.mentify.repository.LessonRepository;
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
import java.util.List;
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
    private LessonRepository lessonRepository;

    @Mock
    private LearningMaterialRepository learningMaterialRepository;

    @Mock
    private TeacherCourseAccessGuard teacherCourseAccessGuard;

    private ModuleServiceImpl moduleService;

    @BeforeEach
    void setUp() {
        moduleService = new ModuleServiceImpl(
                courseRepository,
                moduleRepository,
                lessonRepository,
                learningMaterialRepository,
                teacherCourseAccessGuard
        );
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

    @Test
    void deleteModule_whenValidRequest_returnsOk() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        Course course = baseCourse(courseId);
        Module module = Module.builder()
                .title("Module")
                .sequenceOrder(1)
                .course(course)
                .build();
        module.setId(moduleId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.existsByModule_Id(moduleId)).thenReturn(false);
        when(learningMaterialRepository.existsByModule_Id(moduleId)).thenReturn(false);

        ApiResponse<Object> response = moduleService.deleteModule(courseId, moduleId);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Module deleted successfully");
        verify(teacherCourseAccessGuard).assertTeacherOwnsCourse(course);
        verify(moduleRepository).delete(module);
    }

    @Test
    void deleteModule_whenDependentRecordsExist_throwsBadRequest() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        Course course = baseCourse(courseId);
        Module module = Module.builder()
                .title("Module")
                .sequenceOrder(1)
                .course(course)
                .build();
        module.setId(moduleId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.existsByModule_Id(moduleId)).thenReturn(true);

        assertThatThrownBy(() -> moduleService.deleteModule(courseId, moduleId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot delete module with existing lessons or learning materials");
        verify(moduleRepository, never()).delete(module);
    }

    @Test
    void deleteModule_whenModuleNotFound_throwsNotFound() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.deleteModule(courseId, moduleId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Module not found with id: '" + moduleId + "'");
    }

    @Test
    void getModule_whenValidRequest_returnsOk() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        Course course = baseCourse(courseId);
        Module module = Module.builder().title("Algebra").sequenceOrder(1).course(course).isVisible(true).build();
        module.setId(moduleId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));

        ApiResponse<ModuleResponse> response = moduleService.getModule(courseId, moduleId);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Module fetched successfully");
        assertThat(response.getData().getId()).isEqualTo(moduleId);
        verify(teacherCourseAccessGuard).assertTeacherOwnsCourse(course);
    }

    @Test
    void getModules_whenValidRequest_returnsOk() {
        UUID courseId = UUID.randomUUID();
        Course course = baseCourse(courseId);
        Module module = Module.builder().title("Algebra").sequenceOrder(1).course(course).isVisible(true).build();
        module.setId(UUID.randomUUID());

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(moduleRepository.findAllByCourse_Id(courseId)).thenReturn(List.of(module));

        ApiResponse<List<ModuleResponse>> response = moduleService.getModules(courseId);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Modules fetched successfully");
        assertThat(response.getData()).hasSize(1);
        verify(teacherCourseAccessGuard).assertTeacherOwnsCourse(course);
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
