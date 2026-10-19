package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class TeacherAnalyticsResponse {
    UUID courseId;
    String period;
    TeacherAnalyticsSummaryResponse summary;
    List<TeacherGradeDistributionResponse> gradeDistribution;
    List<TeacherAttendanceCorrelationResponse> attendanceCorrelation;
    List<TeacherAtRiskStudentResponse> atRiskStudents;
    TeacherAnalyticsMetricResponse attendance;
    TeacherAnalyticsMetricResponse assignmentPerformance;
    TeacherAnalyticsMetricResponse quizPerformance;
    TeacherAnalyticsMetricResponse examPerformance;
    List<TeacherAnalyticsCourseTrendResponse> courseTrends;
}
