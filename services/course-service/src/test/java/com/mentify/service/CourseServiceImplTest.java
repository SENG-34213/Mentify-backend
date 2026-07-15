package com.mentify.service;

import com.mentify.dto.CourseRequest;
import com.mentify.dto.CourseResponse;
import com.mentify.entity.Course;
import com.mentify.enums.CourseStatus;
import com.mentify.exception.ResourceAlreadyExistsException;
import com.mentify.exception.ResourceNotFoundException;
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
import java.time.LocalDate;
import java.util.Optional;
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
        assertThat(response.getData().getSubject()).isEqualTo("Mathematics");
        assertThat(response.getData().isOnline()).isTrue();
        assertThat(response.getData().getDiscountOfferPercent()).isEqualByComparingTo("20.00");
        assertThat(response.getData().isVisible()).isTrue();

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
        assertThat(savedCourse.getSubject()).isEqualTo("Mathematics");
        assertThat(savedCourse.isOnline()).isTrue();
        assertThat(savedCourse.getDiscountOfferPercent()).isEqualByComparingTo("20.00");
        assertThat(savedCourse.isVisible()).isTrue();
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

    @Test
    void updateCourse_whenRequestIsValid_updatesCourseAndReturnsOkResponse() {
        UUID courseId = UUID.randomUUID();
        UUID gradeId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        Course existingCourse = Course.builder()
                .courseName("Mathematics")
                .courseDescription("Grade 10 mathematics")
                .courseThumbnail("math.png")
                .courseFeeMonthly(new BigDecimal("2500.00"))
                .gradeId(gradeId)
                .assignedTeacherId(teacherId)
                .subject("Mathematics")
                .isOnline(true)
                .discountOfferPercent(new BigDecimal("20.00"))
                .isVisible(true)
                .isPublished(false)
                .status(CourseStatus.DRAFT)
                .numberOfStudents(28)
                .build();
        existingCourse.setId(courseId);

        CourseRequest updateRequest = CourseRequest.builder()
                .courseName("  Advanced Mathematics  ")
                .courseDescription("  Updated description  ")
                .courseThumbnail("math-v2.png")
                .courseFeeMonthly(new BigDecimal("3000.00"))
                .gradeId(gradeId)
                .assignedTeacherId(teacherId)
                .subject("  Mathematics  ")
                .isOnline(false)
                .discountOfferPercent(new BigDecimal("10.00"))
                .isVisible(true)
                .isPublished(true)
                .build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(courseRepository.existsByCourseNameAndGradeIdAndIdNot(updateRequest.getCourseName(), updateRequest.getGradeId(), courseId))
                .thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<CourseResponse> response = courseService.updateCourse(courseId, updateRequest);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Course updated successfully");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getId()).isEqualTo(courseId);
        assertThat(response.getData().getCourseName()).isEqualTo("Advanced Mathematics");
        assertThat(response.getData().getCourseDescription()).isEqualTo("Updated description");
        assertThat(response.getData().getCourseStatus()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(response.getData().isPublished()).isTrue();
        assertThat(response.getData().getPublishedDate()).isEqualTo(LocalDate.now());
        assertThat(response.getData().getNumberOfStudents()).isEqualTo(28);

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(courseCaptor.capture());
        Course savedCourse = courseCaptor.getValue();
        assertThat(savedCourse.getCourseName()).isEqualTo("Advanced Mathematics");
        assertThat(savedCourse.getCourseDescription()).isEqualTo("Updated description");
        assertThat(savedCourse.getCourseThumbnail()).isEqualTo("math-v2.png");
        assertThat(savedCourse.getCourseFeeMonthly()).isEqualByComparingTo("3000.00");
        assertThat(savedCourse.getSubject()).isEqualTo("Mathematics");
        assertThat(savedCourse.isOnline()).isFalse();
        assertThat(savedCourse.getDiscountOfferPercent()).isEqualByComparingTo("10.00");
        assertThat(savedCourse.isVisible()).isTrue();
        assertThat(savedCourse.isPublished()).isTrue();
        assertThat(savedCourse.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(savedCourse.getPublishedDate()).isEqualTo(LocalDate.now());
        assertThat(savedCourse.getNumberOfStudents()).isEqualTo(28);
    }

    @Test
    void updateCourse_whenCourseDoesNotExist_throwsResourceNotFoundException() {
        UUID courseId = UUID.randomUUID();
        CourseRequest request = validRequest();

        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.updateCourse(courseId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Course not found with id: '" + courseId + "'");

        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCourse_whenCourseNameExistsForAnotherCourse_throwsResourceAlreadyExistsException() {
        UUID courseId = UUID.randomUUID();
        CourseRequest request = validRequest();
        Course existingCourse = CourseMapperStub.existingCourse(courseId, request.getGradeId(), request.getAssignedTeacherId());

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(courseRepository.existsByCourseNameAndGradeIdAndIdNot(request.getCourseName(), request.getGradeId(), courseId))
                .thenReturn(true);

        assertThatThrownBy(() -> courseService.updateCourse(courseId, request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Course already exists with courseName: '" + request.getCourseName() + "'");

        verify(courseRepository, never()).save(any());
    }

    @Test
    void deleteCourse_whenCourseExists_deletesCourseAndReturnsOkResponse() {
        UUID courseId = UUID.randomUUID();
        Course existingCourse = CourseMapperStub.existingCourse(courseId, UUID.randomUUID(), UUID.randomUUID());

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(existingCourse));

        ApiResponse<Object> response = courseService.deleteCourse(courseId);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getMessage()).isEqualTo("Course deleted successfully");
        verify(courseRepository).delete(existingCourse);
    }

    @Test
    void deleteCourse_whenCourseDoesNotExist_throwsResourceNotFoundException() {
        UUID courseId = UUID.randomUUID();

        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.deleteCourse(courseId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Course not found with id: '" + courseId + "'");

        verify(courseRepository, never()).delete(any());
    }

    private static final class CourseMapperStub {
        private static Course existingCourse(UUID courseId, UUID gradeId, UUID teacherId) {
            Course course = Course.builder()
                    .courseName("Mathematics")
                    .courseDescription("Grade 10 mathematics")
                    .courseThumbnail("math.png")
                    .courseFeeMonthly(new BigDecimal("2500.00"))
                    .gradeId(gradeId)
                    .assignedTeacherId(teacherId)
                    .subject("Mathematics")
                    .isOnline(true)
                    .discountOfferPercent(new BigDecimal("20.00"))
                    .isVisible(true)
                    .isPublished(false)
                    .status(CourseStatus.DRAFT)
                    .numberOfStudents(12)
                    .build();
            course.setId(courseId);
            return course;
        }
    }

    private CourseRequest validRequest() {
        return CourseRequest.builder()
                .courseName("  Mathematics  ")
                .courseDescription("  Grade 10 mathematics  ")
                .courseThumbnail("https://cdn.example.com/math.png")
                .courseFeeMonthly(new BigDecimal("2500.00"))
                .gradeId(UUID.randomUUID())
                .assignedTeacherId(UUID.randomUUID())
                .subject("Mathematics")
                .isOnline(true)
                .discountOfferPercent(new BigDecimal("20.00"))
                .isVisible(true)
                .isPublished(false)
                .build();
    }
}
