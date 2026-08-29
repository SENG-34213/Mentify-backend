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

import java.math.BigDecimal;
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

    @Test
    void scopedFinders_andDeleteWorkAsExpected() {
        Course course = courseRepository.saveAndFlush(baseCourse());

        Module module = Module.builder()
                .title("M1")
                .sequenceOrder(1)
                .isVisible(true)
                .course(course)
                .build();
        module = moduleRepository.saveAndFlush(module);

        Lesson lesson = Lesson.builder()
                .title("L1")
                .isVisible(true)
                .module(module)
                .build();
        lesson = lessonRepository.saveAndFlush(lesson);

        LearningMaterial material = LearningMaterial.builder()
                .title("Doc")
                .type(MaterialType.DOC)
                .fileUrl("https://cdn.example.com/doc")
                .module(module)
                .lesson(lesson)
                .build();
        material = learningMaterialRepository.saveAndFlush(material);

        Optional<Module> foundModule = moduleRepository.findByIdAndCourse_Id(module.getId(), course.getId());
        Optional<Lesson> foundLesson = lessonRepository.findByIdAndModule_Id(lesson.getId(), module.getId());
        Optional<LearningMaterial> foundMaterial = learningMaterialRepository.findByIdAndModule_Id(material.getId(), module.getId());

        assertThat(foundModule).isPresent();
        assertThat(foundLesson).isPresent();
        assertThat(foundMaterial).isPresent();

        learningMaterialRepository.delete(foundMaterial.get());
        learningMaterialRepository.flush();

        assertThat(learningMaterialRepository.findByIdAndModule_Id(material.getId(), module.getId())).isEmpty();
    }

    private Course baseCourse() {
        return Course.builder()
                .courseName("Mathematics")
                .courseDescription("Grade 10")
                .courseFeeMonthly(new BigDecimal("2500.00"))
                .subject("Math")
                .gradeId(UUID.randomUUID())
                .assignedTeacherId(UUID.randomUUID())
                .status(CourseStatus.DRAFT)
                .isPublished(false)
                .numberOfStudents(0)
                .build();
    }
}
