package com.mentify.analytics.service.impl;

import com.mentify.analytics.client.CourseAnalyticsClient;
import com.mentify.analytics.client.AttendanceAnalyticsClient;
import com.mentify.analytics.client.AssignmentAnalyticsClient;
import com.mentify.analytics.client.ExamAnalyticsClient;
import com.mentify.analytics.client.EnrollmentAnalyticsClient;
import com.mentify.analytics.client.PaymentAnalyticsClient;
import com.mentify.analytics.client.QuizAnalyticsClient;
import com.mentify.analytics.dto.AssessmentAnalyticsSummary;
import com.mentify.analytics.dto.AttendanceAnalyticsSummary;
import com.mentify.analytics.client.UserAnalyticsClient;
import com.mentify.analytics.dto.CourseAnalyticsOverview;
import com.mentify.analytics.dto.EnrollmentAnalyticsOverview;
import com.mentify.analytics.dto.PaymentAnalyticsOverview;
import com.mentify.analytics.dto.UserAnalyticsOverview;
import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceImplTest {

    @Mock
    private UserAnalyticsClient userAnalyticsClient;

    @Mock
    private CourseAnalyticsClient courseAnalyticsClient;

    @Mock
    private EnrollmentAnalyticsClient enrollmentAnalyticsClient;

    @Mock
    private PaymentAnalyticsClient paymentAnalyticsClient;

    @Mock
    private AttendanceAnalyticsClient attendanceAnalyticsClient;

    @Mock
    private AssignmentAnalyticsClient assignmentAnalyticsClient;

    @Mock
    private QuizAnalyticsClient quizAnalyticsClient;

    @Mock
    private ExamAnalyticsClient examAnalyticsClient;

    @InjectMocks
    private AdminAnalyticsServiceImpl service;

    @Test
    void returnsAllProviderSectionsWhenEveryProviderResponds() {
        UserAnalyticsOverview users = new UserAnalyticsOverview(10, 6, 3, 1, Map.of("ACTIVE", 10L));
        CourseAnalyticsOverview courses = new CourseAnalyticsOverview(4, 3, 1);
        EnrollmentAnalyticsOverview enrollment = new EnrollmentAnalyticsOverview(12, 8, 2, List.of(), Map.of());
        PaymentAnalyticsOverview payments = new PaymentAnalyticsOverview(BigDecimal.valueOf(1000), 7, 1, 0);
        AttendanceAnalyticsSummary attendance = new AttendanceAnalyticsSummary(BigDecimal.valueOf(92));
        AssessmentAnalyticsSummary assessment = new AssessmentAnalyticsSummary(
                BigDecimal.valueOf(80), BigDecimal.valueOf(75), BigDecimal.valueOf(20));
        stubSuccess(users, courses, enrollment, payments, attendance, assessment);

        var result = service.getOverview();

        assertThat(result.users()).isEqualTo(users);
        assertThat(result.courses()).isEqualTo(courses);
        assertThat(result.enrollment()).isEqualTo(enrollment);
        assertThat(result.payments()).isEqualTo(payments);
        assertThat(result.attendance()).isEqualTo(attendance);
        assertThat(result.assignments()).isEqualTo(assessment);
        assertThat(result.quizzes()).isEqualTo(assessment);
        assertThat(result.exams()).isEqualTo(assessment);
        assertThat(result.partial()).isFalse();
        assertThat(result.unavailableSections()).isEmpty();
    }

    @Test
    void preservesAvailableSectionsWhenOneProviderFails() {
        UserAnalyticsOverview users = new UserAnalyticsOverview(10, 6, 3, 1, Map.of());
        when(userAnalyticsClient.getOverview()).thenReturn(ApiResponse.success(200, "ok", users));
        when(courseAnalyticsClient.getOverview()).thenThrow(FeignException.ServiceUnavailable.class);
        when(enrollmentAnalyticsClient.getOverview()).thenReturn(
                ApiResponse.success(200, "ok", new EnrollmentAnalyticsOverview(2, 2, 1, List.of(), Map.of())));
        when(paymentAnalyticsClient.getOverview()).thenReturn(
                ApiResponse.success(200, "ok", new PaymentAnalyticsOverview(BigDecimal.TEN, 1, 0, 0)));
        when(attendanceAnalyticsClient.getOverview()).thenReturn(
                ApiResponse.success(200, "ok", new AttendanceAnalyticsSummary(BigDecimal.valueOf(90))));
        when(assignmentAnalyticsClient.getOverview()).thenThrow(FeignException.ServiceUnavailable.class);
        when(quizAnalyticsClient.getOverview()).thenReturn(
                ApiResponse.success(200, "ok", new AssessmentAnalyticsSummary(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO)));
        when(examAnalyticsClient.getOverview()).thenReturn(
                ApiResponse.success(200, "ok", new AssessmentAnalyticsSummary(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO)));

        var result = service.getOverview();

        assertThat(result.users()).isEqualTo(users);
        assertThat(result.courses()).isNull();
        assertThat(result.enrollment()).isNotNull();
        assertThat(result.payments()).isNotNull();
        assertThat(result.partial()).isTrue();
        assertThat(result.unavailableSections()).containsExactly("courses", "assignments");
    }

    private void stubSuccess(
            UserAnalyticsOverview users,
            CourseAnalyticsOverview courses,
            EnrollmentAnalyticsOverview enrollment,
            PaymentAnalyticsOverview payments,
            AttendanceAnalyticsSummary attendance,
            AssessmentAnalyticsSummary assessment
    ) {
        when(userAnalyticsClient.getOverview()).thenReturn(ApiResponse.success(200, "ok", users));
        when(courseAnalyticsClient.getOverview()).thenReturn(ApiResponse.success(200, "ok", courses));
        when(enrollmentAnalyticsClient.getOverview()).thenReturn(ApiResponse.success(200, "ok", enrollment));
        when(paymentAnalyticsClient.getOverview()).thenReturn(ApiResponse.success(200, "ok", payments));
        when(attendanceAnalyticsClient.getOverview()).thenReturn(ApiResponse.success(200, "ok", attendance));
        when(assignmentAnalyticsClient.getOverview()).thenReturn(ApiResponse.success(200, "ok", assessment));
        when(quizAnalyticsClient.getOverview()).thenReturn(ApiResponse.success(200, "ok", assessment));
        when(examAnalyticsClient.getOverview()).thenReturn(ApiResponse.success(200, "ok", assessment));
    }
}
