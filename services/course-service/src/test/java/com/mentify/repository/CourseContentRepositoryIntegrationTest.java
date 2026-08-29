package com.mentify.repository;

import com.mentify.entity.Course;
import com.mentify.entity.LearningMaterial;
import com.mentify.entity.Lesson;
import com.mentify.entity.Module;
import com.mentify.enums.CourseStatus;
import com.mentify.enums.MaterialType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CourseContentRepositoryIntegrationTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LearningMaterialRepository learningMaterialRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveModuleLessonAndLearningMaterial_persistsHierarchy() {
        Course savedCourse = courseRepository.saveAndFlush(baseCourse(UUID.randomUUID()));

        Module module = Module.builder()
                .title("Algebra")
                .description("Algebra foundations")
                .sequenceOrder(1)
                .dateDuration(30)
                .isVisible(true)
                .course(savedCourse)
                .build();
        Module savedModule = moduleRepository.saveAndFlush(module);

        Lesson lesson = Lesson.builder()
                .title("Linear Equations")
                .description("Solve linear equations")
                .isVisible(true)
                .releaseDate(LocalDate.now())
                .module(savedModule)
                .build();
        Lesson savedLesson = lessonRepository.saveAndFlush(lesson);

        LearningMaterial material = LearningMaterial.builder()
                .title("Equation Video")
                .type(MaterialType.VIDEO)
                .fileUrl("https://cdn.example.com/equation-video.mp4")
                .module(savedModule)
                .lesson(savedLesson)
                .build();
        LearningMaterial savedMaterial = learningMaterialRepository.saveAndFlush(material);

        entityManager.clear();

        Module foundModule = moduleRepository.findById(savedModule.getId()).orElseThrow();
        Lesson foundLesson = lessonRepository.findById(savedLesson.getId()).orElseThrow();
        LearningMaterial foundMaterial = learningMaterialRepository.findById(savedMaterial.getId()).orElseThrow();

        assertThat(foundModule.getCourse().getId()).isEqualTo(savedCourse.getId());
        assertThat(foundLesson.getModule().getId()).isEqualTo(savedModule.getId());
        assertThat(foundMaterial.getModule().getId()).isEqualTo(savedModule.getId());
        assertThat(foundMaterial.getLesson().getId()).isEqualTo(savedLesson.getId());
        assertThat(foundMaterial.getType()).isEqualTo(MaterialType.VIDEO);
    }

    @Test
    void repositoryFinders_scopeByParentRelationships() {
        Course course = courseRepository.saveAndFlush(baseCourse(UUID.randomUUID()));

        Module module = Module.builder()
                .title("Geometry")
                .sequenceOrder(2)
                .isVisible(true)
                .course(course)
                .build();
        module = moduleRepository.saveAndFlush(module);

        Lesson lesson = Lesson.builder()
                .title("Triangles")
                .isVisible(true)
                .module(module)
                .build();
        lesson = lessonRepository.saveAndFlush(lesson);

        LearningMaterial material = LearningMaterial.builder()
                .title("Triangles PDF")
                .type(MaterialType.PDF)
                .fileUrl("https://cdn.example.com/triangles.pdf")
                .module(module)
                .lesson(lesson)
                .build();
        material = learningMaterialRepository.saveAndFlush(material);

        Optional<Module> moduleByCourse = moduleRepository.findByIdAndCourse_Id(module.getId(), course.getId());
        Optional<Module> moduleByWrongCourse = moduleRepository.findByIdAndCourse_Id(module.getId(), UUID.randomUUID());

        Optional<Lesson> lessonByModule = lessonRepository.findByIdAndModule_Id(lesson.getId(), module.getId());
        Optional<Lesson> lessonByWrongModule = lessonRepository.findByIdAndModule_Id(lesson.getId(), UUID.randomUUID());

        Optional<LearningMaterial> materialByModule =
                learningMaterialRepository.findByIdAndModule_Id(material.getId(), module.getId());
        Optional<LearningMaterial> materialByWrongModule =
                learningMaterialRepository.findByIdAndModule_Id(material.getId(), UUID.randomUUID());

        assertThat(moduleByCourse).isPresent();
        assertThat(moduleByWrongCourse).isEmpty();
        assertThat(lessonByModule).isPresent();
        assertThat(lessonByWrongModule).isEmpty();
                assertThat(materialByModule).isPresent();
                assertThat(materialByWrongModule).isEmpty();
    }

    @Test
    void repositoryDelete_operationsRemoveEntities() {
        Course course = courseRepository.saveAndFlush(baseCourse(UUID.randomUUID()));

        Module module = Module.builder()
                .title("Calculus")
                .sequenceOrder(3)
                .isVisible(true)
                .course(course)
                .build();
        module = moduleRepository.saveAndFlush(module);

        Lesson lesson = Lesson.builder()
                .title("Limits")
                .isVisible(true)
                .module(module)
                .build();
        lesson = lessonRepository.saveAndFlush(lesson);

        LearningMaterial material = LearningMaterial.builder()
                .title("Limits Notes")
                .type(MaterialType.PDF)
                .fileUrl("https://cdn.example.com/limits.pdf")
                .module(module)
                .lesson(lesson)
                .build();
        material = learningMaterialRepository.saveAndFlush(material);

        learningMaterialRepository.delete(material);
        learningMaterialRepository.flush();

        lessonRepository.delete(lesson);
        lessonRepository.flush();

        moduleRepository.delete(module);
        moduleRepository.flush();

        assertThat(learningMaterialRepository.findById(material.getId())).isEmpty();
        assertThat(lessonRepository.findById(lesson.getId())).isEmpty();
        assertThat(moduleRepository.findById(module.getId())).isEmpty();
    }

    private Course baseCourse(UUID teacherId) {
        return Course.builder()
                .courseName("Mathematics")
                .courseDescription("Grade 10 mathematics")
                .courseFeeMonthly(new BigDecimal("2500.00"))
                .subject("Mathematics")
                .assignedTeacherId(teacherId)
                .gradeId(UUID.randomUUID())
                .status(CourseStatus.DRAFT)
                .isPublished(false)
                .numberOfStudents(0)
                .build();
    }
}
