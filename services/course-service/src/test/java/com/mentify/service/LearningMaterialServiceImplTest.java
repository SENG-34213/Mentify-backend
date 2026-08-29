package com.mentify.service;

import com.mentify.dto.LearningMaterialCreateRequest;
import com.mentify.dto.LearningMaterialResponse;
import com.mentify.entity.Course;
import com.mentify.entity.LearningMaterial;
import com.mentify.entity.Lesson;
import com.mentify.entity.Module;
import com.mentify.enums.CourseStatus;
import com.mentify.enums.MaterialType;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.LearningMaterialRepository;
import com.mentify.repository.LessonRepository;
import com.mentify.repository.ModuleRepository;
import com.mentify.service.impl.LearningMaterialServiceImpl;
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
class LearningMaterialServiceImplTest {

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LearningMaterialRepository learningMaterialRepository;

    @Mock
    private TeacherCourseAccessGuard teacherCourseAccessGuard;

    private LearningMaterialServiceImpl learningMaterialService;

    @BeforeEach
    void setUp() {
        learningMaterialService = new LearningMaterialServiceImpl(
                moduleRepository,
                lessonRepository,
                learningMaterialRepository,
                teacherCourseAccessGuard
        );
    }

    @Test
    void createLearningMaterial_whenValidRequest_returnsCreated() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        Module module = baseModule(courseId, moduleId);

        Lesson lesson = Lesson.builder()
                .title("Lesson 1")
                .isVisible(true)
                .module(module)
                .build();
        lesson.setId(lessonId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.findByIdAndModule_Id(lessonId, moduleId)).thenReturn(Optional.of(lesson));
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenAnswer(invocation -> {
            LearningMaterial material = invocation.getArgument(0);
            material.setId(UUID.randomUUID());
            return material;
        });

        ApiResponse<LearningMaterialResponse> response = learningMaterialService.createLearningMaterial(
                courseId,
                moduleId,
                LearningMaterialCreateRequest.builder()
                        .title("  Intro Video  ")
                        .type(MaterialType.VIDEO)
                        .fileUrl("  https://cdn.example.com/video.mp4  ")
                        .lessonId(lessonId)
                        .build()
        );

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getTitle()).isEqualTo("Intro Video");
        assertThat(response.getData().getLessonId()).isEqualTo(lessonId);

        verify(teacherCourseAccessGuard).assertTeacherOwnsCourse(module.getCourse());
    }

    @Test
    void createLearningMaterial_whenLessonNotFound_throwsNotFound() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        Module module = baseModule(courseId, moduleId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.findByIdAndModule_Id(lessonId, moduleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningMaterialService.createLearningMaterial(
                courseId,
                moduleId,
                LearningMaterialCreateRequest.builder()
                        .title("Intro Video")
                        .type(MaterialType.VIDEO)
                        .fileUrl("https://cdn.example.com/video.mp4")
                        .lessonId(lessonId)
                        .build()
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Lesson not found with id: '" + lessonId + "'");
    }

    @Test
    void updateLearningMaterial_whenValidRequest_returnsOk() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        Module module = baseModule(courseId, moduleId);

        Lesson lesson = Lesson.builder()
                .title("Lesson 1")
                .isVisible(true)
                .module(module)
                .build();
        lesson.setId(lessonId);

        LearningMaterial material = LearningMaterial.builder()
                .title("Old Video")
                .type(MaterialType.VIDEO)
                .fileUrl("https://cdn.example.com/old.mp4")
                .module(module)
                .build();
        material.setId(materialId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(learningMaterialRepository.findByIdAndModule_Id(materialId, moduleId)).thenReturn(Optional.of(material));
        when(lessonRepository.findByIdAndModule_Id(lessonId, moduleId)).thenReturn(Optional.of(lesson));
        when(learningMaterialRepository.save(any(LearningMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<LearningMaterialResponse> response = learningMaterialService.updateLearningMaterial(
                courseId,
                moduleId,
                materialId,
                LearningMaterialCreateRequest.builder()
                        .title("  Updated Video  ")
                        .type(MaterialType.PDF)
                        .fileUrl("  https://cdn.example.com/new.pdf  ")
                        .lessonId(lessonId)
                        .build()
        );

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Learning material updated successfully");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getTitle()).isEqualTo("Updated Video");
        assertThat(response.getData().getType()).isEqualTo(MaterialType.PDF);
        assertThat(response.getData().getLessonId()).isEqualTo(lessonId);

        verify(teacherCourseAccessGuard).assertTeacherOwnsCourse(module.getCourse());
    }

    @Test
    void updateLearningMaterial_whenMaterialNotFound_throwsNotFound() {
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        Module module = baseModule(courseId, moduleId);

        when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
        when(learningMaterialRepository.findByIdAndModule_Id(materialId, moduleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningMaterialService.updateLearningMaterial(
                courseId,
                moduleId,
                materialId,
                LearningMaterialCreateRequest.builder()
                        .title("Updated Video")
                        .type(MaterialType.VIDEO)
                        .fileUrl("https://cdn.example.com/video.mp4")
                        .build()
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("LearningMaterial not found with id: '" + materialId + "'");
    }

        @Test
        void deleteLearningMaterial_whenValidRequest_returnsOk() {
                UUID courseId = UUID.randomUUID();
                UUID moduleId = UUID.randomUUID();
                UUID materialId = UUID.randomUUID();
                Module module = baseModule(courseId, moduleId);

                LearningMaterial material = LearningMaterial.builder()
                                .title("Material")
                                .type(MaterialType.VIDEO)
                                .fileUrl("https://cdn.example.com/video.mp4")
                                .module(module)
                                .build();
                material.setId(materialId);

                when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
                when(learningMaterialRepository.findByIdAndModule_Id(materialId, moduleId)).thenReturn(Optional.of(material));

                ApiResponse<Object> response = learningMaterialService.deleteLearningMaterial(courseId, moduleId, materialId);

                assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
                assertThat(response.getMessage()).isEqualTo("Learning material deleted successfully");
                verify(teacherCourseAccessGuard).assertTeacherOwnsCourse(module.getCourse());
                verify(learningMaterialRepository).delete(material);
        }

        @Test
        void deleteLearningMaterial_whenMaterialNotFound_throwsNotFound() {
                UUID courseId = UUID.randomUUID();
                UUID moduleId = UUID.randomUUID();
                UUID materialId = UUID.randomUUID();
                Module module = baseModule(courseId, moduleId);

                when(moduleRepository.findByIdAndCourse_Id(moduleId, courseId)).thenReturn(Optional.of(module));
                when(learningMaterialRepository.findByIdAndModule_Id(materialId, moduleId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> learningMaterialService.deleteLearningMaterial(courseId, moduleId, materialId))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessage("LearningMaterial not found with id: '" + materialId + "'");
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
