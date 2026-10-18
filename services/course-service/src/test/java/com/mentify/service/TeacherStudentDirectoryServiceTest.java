package com.mentify.service;

import com.mentify.client.EntrollmentServiceClient;
import com.mentify.client.UserServiceClient;
import com.mentify.client.dto.StudentDirectoryProfileResponse;
import com.mentify.dto.TeacherStudentDirectoryResponse;
import com.mentify.entity.Course;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.service.impl.TeacherCourseAccessGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherStudentDirectoryServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EntrollmentServiceClient entrollmentServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private TeacherCourseAccessGuard teacherCourseAccessGuard;

    @InjectMocks
    private TeacherStudentDirectoryService directoryService;

    @Test
    void getStudents_resolvesEnrolledIdsToProfilesAndPreservesCourseScope() {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        String authorization = "Bearer token";
        Course course = Course.builder().courseName("Mathematics").build();
        course.setId(courseId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(teacherCourseAccessGuard.isTeacherAuthenticated()).thenReturn(false);
        when(entrollmentServiceClient.getEnrolledStudentIdsByCourse(courseId, authorization))
                .thenReturn(ApiResponse.<List<UUID>>builder().data(List.of(studentId, studentId)).build());
        StudentDirectoryProfileResponse profile = new StudentDirectoryProfileResponse();
        profile.setId(studentId);
        profile.setName("Student Name");
        profile.setEmail("student@example.com");
        profile.setStudentNumber("STU-001");
        profile.setStatus("ACTIVE");
        when(userServiceClient.getStudentsByIds(List.of(studentId), authorization))
                .thenReturn(ApiResponse.<List<StudentDirectoryProfileResponse>>builder()
                        .data(List.of(profile)).build());

        ApiResponse<List<TeacherStudentDirectoryResponse>> response =
                directoryService.getStudents(courseId, authorization);

        assertThat(response.getData()).singleElement().satisfies(student -> {
            assertThat(student.getId()).isEqualTo(studentId);
            assertThat(student.getName()).isEqualTo("Student Name");
            assertThat(student.getCourses()).containsExactly(courseId);
        });
        verify(userServiceClient).getStudentsByIds(eq(List.of(studentId)), eq(authorization));
    }
}
