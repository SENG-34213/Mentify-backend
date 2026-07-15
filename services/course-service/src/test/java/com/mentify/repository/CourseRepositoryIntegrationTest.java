package com.mentify.repository;

import com.mentify.entity.Course;
import com.mentify.enums.CourseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CourseRepositoryIntegrationTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveAndFindCourse_persistsCourseMetadataAndAssignmentDetails() {
        UUID teacherId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();

        Course course = Course.builder()
                .courseName("Physics")
                .courseDescription("Advanced physics for grade 11")
                .courseThumbnail("physics.png")
                .courseFeeMonthly(new BigDecimal("1800.00"))
                .subject("Physics")
                .isOnline(true)
                .discountOfferPercent(new BigDecimal("20.00"))
                .isVisible(true)
                .assignedTeacherId(teacherId)
                .courseEnrollmentId(UUID.randomUUID())
                .gradeId(gradeId)
                .isPublished(false)
                .status(CourseStatus.DRAFT)
                .numberOfStudents(0)
                .build();

        Course savedCourse = courseRepository.saveAndFlush(course);
        entityManager.clear();

        Course foundCourse = courseRepository.findById(savedCourse.getId()).orElseThrow();

        assertThat(foundCourse.getCourseName()).isEqualTo("Physics");
        assertThat(foundCourse.getAssignedTeacherId()).isEqualTo(teacherId);
        assertThat(foundCourse.getGradeId()).isEqualTo(gradeId);
        assertThat(foundCourse.getSubject()).isEqualTo("Physics");
        assertThat(foundCourse.isOnline()).isTrue();
        assertThat(foundCourse.getDiscountOfferPercent()).isEqualByComparingTo("20.00");
        assertThat(foundCourse.isVisible()).isTrue();
        assertThat(foundCourse.getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    void existsByCourseNameAndGradeIdAndIdNot_whenSameNameAndGradeOnAnotherCourse_returnsTrue() {
        UUID gradeId = UUID.randomUUID();

        Course existingCourse = Course.builder()
                .courseName("Physics")
                .courseDescription("Physics course")
                .courseThumbnail("physics.png")
                .courseFeeMonthly(new BigDecimal("1800.00"))
                .subject("Physics")
                .assignedTeacherId(UUID.randomUUID())
                .gradeId(gradeId)
                .status(CourseStatus.DRAFT)
                .build();
        existingCourse = courseRepository.saveAndFlush(existingCourse);

        boolean existsForAnotherId = courseRepository.existsByCourseNameAndGradeIdAndIdNot(
                "Physics",
                gradeId,
                UUID.randomUUID()
        );

        boolean existsForSameId = courseRepository.existsByCourseNameAndGradeIdAndIdNot(
                "Physics",
                gradeId,
                existingCourse.getId()
        );

        assertThat(existsForAnotherId).isTrue();
        assertThat(existsForSameId).isFalse();
    }
}
