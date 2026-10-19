package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TeacherAnalyticsResponse {
    TeacherAnalyticsMetricResponse attendance;
    TeacherAnalyticsMetricResponse assignmentPerformance;
    TeacherAnalyticsMetricResponse quizPerformance;
    TeacherAnalyticsMetricResponse examPerformance;
    List<TeacherAnalyticsCourseTrendResponse> courseTrends;
}
