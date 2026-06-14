package com.mentify.service;

import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.entity.Course;
import com.mentify.enums.CourseStatus;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    private CourseServiceImpl courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseServiceImpl(courseRepository);
    }

    @Test
    void createCourse_whenRequestIsValid_savesDraftCourseAndReturnsCreatedResponse() {
        CourseRequest request = validRequest();
        UUID courseId = UUID.randomUUID();

        when(courseRepository.existsByCourseNameAndGradeId(request.getCourseName(), request.getGradeId()))
                .thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            course.setId(courseId);
            return course;
        });

        ApiResponse<CourseResponse> response = courseService.createCourse(request);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getMessage()).isEqualTo("Course created successfully");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getId()).isEqualTo(courseId);
        assertThat(response.getData().getCourseName()).isEqualTo("Mathematics");
        assertThat(response.getData().getCourseDescription()).isEqualTo("Grade 10 mathematics");
        assertThat(response.getData().getCourseStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(response.getData().isPublished()).isFalse();
        assertThat(response.getData().getNumberOfStudents()).isZero();

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(courseCaptor.capture());
        Course savedCourse = courseCaptor.getValue();
        assertThat(savedCourse.getCourseName()).isEqualTo("Mathematics");
        assertThat(savedCourse.getCourseDescription()).isEqualTo("Grade 10 mathematics");
        assertThat(savedCourse.getCourseThumbnail()).isEqualTo(request.getCourseThumbnail());
        assertThat(savedCourse.getCourseFeeMonthly()).isEqualByComparingTo(request.getCourseFeeMonthly());
        assertThat(savedCourse.getGradeId()).isEqualTo(request.getGradeId());
        assertThat(savedCourse.getAssignedTeacherId()).isEqualTo(request.getAssignedTeacherId());
        assertThat(savedCourse.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(savedCourse.isPublished()).isFalse();
        assertThat(savedCourse.getNumberOfStudents()).isZero();
    }

    @Test
    void createCourse_whenCourseNameAlreadyExistsForGrade_throwsResourceAlreadyExistsException() {
        CourseRequest request = validRequest();
        request.setCourseName("Mathematics");

        when(courseRepository.existsByCourseNameAndGradeId(request.getCourseName(), request.getGradeId()))
                .thenReturn(true);

        assertThatThrownBy(() -> courseService.createCourse(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Course already exists with courseName: 'Mathematics'");

        verify(courseRepository, never()).save(any());
    }

    private CourseRequest validRequest() {
        return CourseRequest.builder()
                .courseName("  Mathematics  ")
                .courseDescription("  Grade 10 mathematics  ")
                .courseThumbnail("https://cdn.example.com/math.png")
                .courseFeeMonthly(new BigDecimal("2500.00"))
                .gradeId(UUID.randomUUID())
                .assignedTeacherId(UUID.randomUUID())
                .build();
    }
}
