package com.mentify.exam.service;

import com.mentify.exam.dto.response.ExamSummaryResponse;

import java.util.UUID;

public interface ExamSummaryService {

    /** Read-only staff summary aggregated from stored results; nothing is recalculated or persisted. */
    ExamSummaryResponse getSummary(UUID examId, String authorizationHeader);
}