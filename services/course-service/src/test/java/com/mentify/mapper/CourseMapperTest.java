package com.mentify.mapper;

import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.entity.Course;
import com.mentify.entity.Grade;
import com.mentify.enums.CourseStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Map;
import java.util.Set;

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
                .gradeId(gradeId)
                .assignedTeacherId(teacherId)
                .credits(3)
                .schedule("Monday 09:00-11:00")
                .gradingWeights(Map.of("ASSIGNMENT", new BigDecimal("40.00")))
                .teachingAssistantIds(Set.of(UUID.randomUUID()))
                .build();

        Course course = CourseMapper.toCourseEntity(request);

        assertThat(course.getCourseName()).isEqualTo("Science");
        assertThat(course.getCourseDescription()).isEqualTo("Grade 9 science");
        assertThat(course.getCourseThumbnail()).isEqualTo("science.png");
        assertThat(course.getGradeId()).isEqualTo(gradeId);
        assertThat(course.getAssignedTeacherId()).isEqualTo(teacherId);
        assertThat(course.getCredits()).isEqualTo(3);
        assertThat(course.getSchedule()).isEqualTo("Monday 09:00-11:00");
        assertThat(course.getGradingWeights()).containsEntry("ASSIGNMENT", new BigDecimal("40.00"));
        assertThat(course.getTeachingAssistantIds()).hasSize(1);
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
                .subject("Languages")
                .gradeId(gradeId)
                .assignedTeacherId(teacherId)
                .status(CourseStatus.PUBLISHED)
                .isPublished(true)
                .publishedDate(publishedDate)
                .numberOfStudents(12)
                .credits(4)
                .schedule("Tuesday 10:00-12:00")
                .build();
        course.setId(courseId);

        CourseResponse response = CourseMapper.toCourseResponse(course);

        assertThat(response.getId()).isEqualTo(courseId);
        assertThat(response.getCourseName()).isEqualTo("English");
        assertThat(response.getCourseDescription()).isEqualTo("Spoken English");
        assertThat(response.getCourseThumbnail()).isEqualTo("english.png");
        assertThat(response.getSubject()).isEqualTo("Languages");
        assertThat(response.getGradeId()).isEqualTo(gradeId);
        assertThat(response.getAssignedTeacherId()).isEqualTo(teacherId);
        assertThat(response.getCourseStatus()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(response.isPublished()).isTrue();
        assertThat(response.getPublishedDate()).isEqualTo(publishedDate);
        assertThat(response.getNumberOfStudents()).isEqualTo(12);
        assertThat(response.getCredits()).isEqualTo(4);
        assertThat(response.getSchedule()).isEqualTo("Tuesday 10:00-12:00");
    }

    @Test
    void toCourseResponse_includesGradeNameWhenGradeIsResolved() {
        UUID gradeId = UUID.randomUUID();
        Course course = Course.builder()
                .courseName("English")
                .courseDescription("Spoken English")
                .gradeId(gradeId)
                .assignedTeacherId(UUID.randomUUID())
                .build();
        Grade grade = Grade.builder().name("Grade 10").build();

        CourseResponse response = CourseMapper.toCourseResponse(course, grade);

        assertThat(response.getGradeId()).isEqualTo(gradeId);
        assertThat(response.getGradeName()).isEqualTo("Grade 10");
    }
}
