package com.mentify.exam.service;

import com.mentify.exam.entity.ExamResult;

import java.util.List;
import java.util.UUID;

public interface ExamResultService {

    /**
     * Creates one NOT_MARKED result for every student enrolled in the exam's course that has none yet.
     * Idempotent; returns only the newly created results.
     */
    List<ExamResult> initializeResults(UUID examId, String authorizationHeader);
}
