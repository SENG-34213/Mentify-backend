package com.mentify.mapper;

import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.entity.Course;
import com.mentify.enums.CourseStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseMapperTest {

    @Test
    void toCourseEntity_mapsRequestToDraftCourseDefaults() {
        UUID gradeId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        CourseRequest request = CourseRequest.builder()
                .courseName("  Science  ")
                .courseDescription("  Grade 9 science  ")
                .courseThumbnail("science.png")
                .courseFeeMonthly(new BigDecimal("3000.00"))
                .gradeId(gradeId)
                .assignedTeacherId(teacherId)
                .build();

        Course course = CourseMapper.toCourseEntity(request);

        assertThat(course.getCourseName()).isEqualTo("Science");
        assertThat(course.getCourseDescription()).isEqualTo("Grade 9 science");
        assertThat(course.getCourseThumbnail()).isEqualTo("science.png");
        assertThat(course.getCourseFeeMonthly()).isEqualByComparingTo("3000.00");
        assertThat(course.getGradeId()).isEqualTo(gradeId);
        assertThat(course.getAssignedTeacherId()).isEqualTo(teacherId);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(course.isPublished()).isFalse();
        assertThat(course.getNumberOfStudents()).isZero();
    }

    @Test
    void toCourseResponse_mapsPersistedCourseFields() {
        UUID courseId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        LocalDate publishedDate = LocalDate.now();
        Course course = Course.builder()
                .courseName("English")
                .courseDescription("Spoken English")
                .courseThumbnail("english.png")
                .courseFeeMonthly(new BigDecimal("1500.00"))
                .gradeId(gradeId)
                .assignedTeacherId(teacherId)
                .status(CourseStatus.PUBLISHED)
                .isPublished(true)
                .publishedDate(publishedDate)
                .numberOfStudents(12)
                .build();
        course.setId(courseId);

        CourseResponse response = CourseMapper.toCourseResponse(course);

        assertThat(response.getId()).isEqualTo(courseId);
        assertThat(response.getCourseName()).isEqualTo("English");
        assertThat(response.getCourseDescription()).isEqualTo("Spoken English");
        assertThat(response.getCourseThumbnail()).isEqualTo("english.png");
        assertThat(response.getCourseFeeMonthly()).isEqualByComparingTo("1500.00");
        assertThat(response.getGradeId()).isEqualTo(gradeId);
        assertThat(response.getAssignedTeacherId()).isEqualTo(teacherId);
        assertThat(response.getCourseStatus()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(response.isPublished()).isTrue();
        assertThat(response.getPublishedDate()).isEqualTo(publishedDate);
        assertThat(response.getNumberOfStudents()).isEqualTo(12);
    }
}
