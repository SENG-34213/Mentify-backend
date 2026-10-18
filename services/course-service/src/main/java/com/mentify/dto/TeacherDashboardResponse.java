package com.mentify.dto;

import com.mentify.client.dashboard.DashboardDtos;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TeacherDashboardResponse {
    long courseCount;
    long studentCount;
    long activeAssignmentCount;
    long pendingReviewCount;
    long upcomingExamCount;
    long todayClassCount;
    List<DashboardDtos.CalendarEvent> todaySchedule;
    List<DashboardDtos.PendingGrading> pendingGrading;
    List<DashboardDtos.CoursePerformance> coursePerformance;
}
