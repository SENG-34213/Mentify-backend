package com.mentify.service;

import com.mentify.client.EntrollmentServiceClient;
import com.mentify.client.UserServiceClient;
import com.mentify.client.dto.StudentDirectoryProfileResponse;
import com.mentify.dto.TeacherStudentDirectoryResponse;
import com.mentify.entity.Course;
import com.mentify.exception.ResourceNotFoundException;
import com.mentify.payload.response.ApiResponse;
import com.mentify.repository.CourseRepository;
import com.mentify.service.impl.TeacherCourseAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherStudentDirectoryService {

    private final CourseRepository courseRepository;
    private final EntrollmentServiceClient entrollmentServiceClient;
    private final UserServiceClient userServiceClient;
    private final TeacherCourseAccessGuard teacherCourseAccessGuard;

    public ApiResponse<List<TeacherStudentDirectoryResponse>> getStudents(
            UUID courseId, String authorizationHeader) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        if (teacherCourseAccessGuard.isTeacherAuthenticated()) {
            teacherCourseAccessGuard.assertTeacherOwnsCourse(course);
        }

        ApiResponse<List<UUID>> enrollmentResponse = entrollmentServiceClient
                .getEnrolledStudentIdsByCourse(courseId, authorizationHeader);
        List<UUID> studentIds = enrollmentResponse == null || enrollmentResponse.getData() == null
                ? List.of() : enrollmentResponse.getData().stream().distinct().toList();
        if (studentIds.isEmpty()) {
            return success(List.of());
        }

        ApiResponse<List<StudentDirectoryProfileResponse>> profileResponse = userServiceClient
                .getStudentsByIds(studentIds, authorizationHeader);
        List<StudentDirectoryProfileResponse> profiles = profileResponse == null
                || profileResponse.getData() == null ? List.of() : profileResponse.getData();
        List<TeacherStudentDirectoryResponse> students = profiles.stream()
                .map(profile -> TeacherStudentDirectoryResponse.builder()
                        .id(profile.getId())
                        .name(profile.getName())
                        .email(profile.getEmail())
                        .studentNumber(profile.getStudentNumber())
                        .avatar(profile.getAvatar())
                        .program(profile.getProgram())
                        .status(profile.getStatus())
                        .courses(List.of(courseId))
                        .build())
                .toList();
        return success(students);
    }

    private ApiResponse<List<TeacherStudentDirectoryResponse>> success(
            List<TeacherStudentDirectoryResponse> students) {
        return ApiResponse.<List<TeacherStudentDirectoryResponse>>builder()
                .message("Teacher students fetched successfully")
                .data(students)
                .statusCode(HttpStatus.OK.value())
                .status(HttpStatus.OK)
                .build();
    }
}
