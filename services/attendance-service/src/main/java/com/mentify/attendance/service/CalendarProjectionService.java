package com.mentify.attendance.service;

import com.mentify.attendance.entity.AttendanceSession;

import java.util.UUID;

public interface CalendarProjectionService {
    void projectIfEligible(AttendanceSession session, String authorizationHeader);
    void cancel(AttendanceSession session, String authorizationHeader);
    void reconcile(UUID sessionId, String authorizationHeader);
}
