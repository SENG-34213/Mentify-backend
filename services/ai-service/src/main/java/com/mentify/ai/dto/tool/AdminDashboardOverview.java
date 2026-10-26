package com.mentify.ai.dto.tool;

public record AdminDashboardOverview(
        UserAnalyticsOverview users,
        CourseAnalyticsOverview courses,
        EnrollmentAnalyticsOverview enrollment,
        PaymentAnalyticsOverview payments,
        AttendanceAnalyticsOverview attendance,
        AssessmentAnalyticsOverview assignments,
        AssessmentAnalyticsOverview quizzes,
        AssessmentAnalyticsOverview exams,
        boolean partial,
        java.util.List<String> unavailableSections
) {

    public record UserAnalyticsOverview(
            long totalUsers,
            long students,
            long teachers,
            long admins,
            java.util.Map<String, Long> accountStatusMix
    ) {
    }

    public record AttendanceAnalyticsOverview(java.math.BigDecimal averageAttendance) {
    }

    public record AssessmentAnalyticsOverview(
            java.math.BigDecimal completionRate,
            java.math.BigDecimal averageScore,
            java.math.BigDecimal missingRate
    ) {
    }

    public record CourseAnalyticsOverview(long totalCourses, long activeCourses, long newThisMonth) {
    }

    public record EnrollmentAnalyticsOverview(
            long activeEnrollments,
            long uniqueStudents,
            long newThisMonth,
            java.util.List<java.util.Map<String, Object>> coursePopularity,
            java.util.Map<String, Long> enrollmentTrend
    ) {
    }

    public record PaymentAnalyticsOverview(
            java.math.BigDecimal revenue,
            long successfulPayments,
            long failedPayments,
            long refundedPayments
    ) {
    }
}
