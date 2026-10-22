package com.mentify.analytics.service.impl;

import com.mentify.analytics.client.CourseAnalyticsClient;
import com.mentify.analytics.client.AttendanceAnalyticsClient;
import com.mentify.analytics.client.AssignmentAnalyticsClient;
import com.mentify.analytics.client.ExamAnalyticsClient;
import com.mentify.analytics.client.EnrollmentAnalyticsClient;
import com.mentify.analytics.client.PaymentAnalyticsClient;
import com.mentify.analytics.client.QuizAnalyticsClient;
import com.mentify.analytics.client.UserAnalyticsClient;
import com.mentify.analytics.dto.AdminAnalyticsOverviewResponse;
import com.mentify.analytics.dto.AssessmentAnalyticsSummary;
import com.mentify.analytics.dto.AttendanceAnalyticsSummary;
import com.mentify.analytics.dto.CourseAnalyticsOverview;
import com.mentify.analytics.dto.EnrollmentAnalyticsOverview;
import com.mentify.analytics.dto.PaymentAnalyticsOverview;
import com.mentify.analytics.dto.UserAnalyticsOverview;
import com.mentify.analytics.service.AdminAnalyticsService;
import com.mentify.payload.response.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final UserAnalyticsClient userAnalyticsClient;
    private final CourseAnalyticsClient courseAnalyticsClient;
    private final EnrollmentAnalyticsClient enrollmentAnalyticsClient;
    private final PaymentAnalyticsClient paymentAnalyticsClient;
    private final AttendanceAnalyticsClient attendanceAnalyticsClient;
    private final AssignmentAnalyticsClient assignmentAnalyticsClient;
    private final QuizAnalyticsClient quizAnalyticsClient;
    private final ExamAnalyticsClient examAnalyticsClient;

    @Override
    public AdminAnalyticsOverviewResponse getOverview() {
        List<String> unavailableSections = new ArrayList<>();

        UserAnalyticsOverview users = load(
                "users", userAnalyticsClient::getOverview, unavailableSections);
        CourseAnalyticsOverview courses = load(
                "courses", courseAnalyticsClient::getOverview, unavailableSections);
        EnrollmentAnalyticsOverview enrollment = load(
                "enrollment", enrollmentAnalyticsClient::getOverview, unavailableSections);
        PaymentAnalyticsOverview payments = load(
                "payments", paymentAnalyticsClient::getOverview, unavailableSections);
        AttendanceAnalyticsSummary attendance = load(
                "attendance", attendanceAnalyticsClient::getOverview, unavailableSections);
        AssessmentAnalyticsSummary assignments = load(
                "assignments", assignmentAnalyticsClient::getOverview, unavailableSections);
        AssessmentAnalyticsSummary quizzes = load(
                "quizzes", quizAnalyticsClient::getOverview, unavailableSections);
        AssessmentAnalyticsSummary exams = load(
                "exams", examAnalyticsClient::getOverview, unavailableSections);

        return new AdminAnalyticsOverviewResponse(
                users,
                courses,
                enrollment,
                payments,
                attendance,
                assignments,
                quizzes,
                exams,
                !unavailableSections.isEmpty(),
                List.copyOf(unavailableSections)
        );
    }

    private <T> T load(
            String section,
            Supplier<ApiResponse<T>> providerCall,
            List<String> unavailableSections
    ) {
        try {
            ApiResponse<T> response = providerCall.get();
            if (response == null || response.getData() == null) {
                unavailableSections.add(section);
                return null;
            }
            return response.getData();
        } catch (FeignException exception) {
            log.warn("Analytics provider failed for {} with status {}", section, exception.status());
            unavailableSections.add(section);
            return null;
        } catch (RuntimeException exception) {
            log.warn("Analytics provider unavailable for {}", section, exception);
            unavailableSections.add(section);
            return null;
        }
    }
}
