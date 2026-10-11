package com.mentify.exam.service;

import com.mentify.exam.entity.Exam;

import java.util.UUID;

public interface ExamCalendarProjectionService {
    void projectIfEligible(Exam exam, String authorizationHeader);
    void cancel(Exam exam, String authorizationHeader);
    void reconcile(UUID examId, String authorizationHeader);
}
