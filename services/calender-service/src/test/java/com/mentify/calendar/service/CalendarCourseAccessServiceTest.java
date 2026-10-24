package com.mentify.calendar.service;

import com.mentify.calendar.client.CourseServiceClient;
import com.mentify.calendar.client.EnrollmentServiceClient;
import com.mentify.calendar.client.dto.CourseLookupResponse;
import com.mentify.calendar.security.CurrentCalendarUserService;
import com.mentify.payload.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarCourseAccessServiceTest {

    @Mock
    private CourseServiceClient courseServiceClient;
    @Mock
    private EnrollmentServiceClient enrollmentServiceClient;
    @Mock
    private CurrentCalendarUserService currentUser;
    @InjectMocks
    private CalendarCourseAccessService service;

    @Test
    void assignedTeacherMayManageCourseEvents() {
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        CourseLookupResponse course = new CourseLookupResponse();
        course.setId(courseId);
        course.setAssignedTeacherId(teacherId);
        when(courseServiceClient.getCourseById(courseId, "Bearer token"))
                .thenReturn(ApiResponse.success(200, "ok", course));
        when(currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUser.hasRole("TEACHER")).thenReturn(true);
        when(currentUser.getIdentityIds()).thenReturn(java.util.Set.of(teacherId));

        service.assertCanManage(courseId, "Bearer token");
    }

    @Test
    void unassignedTeacherCannotManageCourseEvents() {
        UUID courseId = UUID.randomUUID();
        CourseLookupResponse course = new CourseLookupResponse();
        course.setId(courseId);
        course.setAssignedTeacherId(UUID.randomUUID());
        when(courseServiceClient.getCourseById(courseId, "Bearer token"))
                .thenReturn(ApiResponse.success(200, "ok", course));
        when(currentUser.hasAnyRole("ADMIN", "SUPER_ADMIN")).thenReturn(false);
        when(currentUser.hasRole("TEACHER")).thenReturn(true);
        when(currentUser.getIdentityIds()).thenReturn(java.util.Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.assertCanManage(courseId, "Bearer token"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
