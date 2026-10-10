package com.mentify.exam.service;

import com.mentify.exam.dto.response.ExamResultSheetResponse;

import java.util.UUID;

public interface ExamResultSheetService {

    /** Read-only staff view of every stored result of an exam; nothing is recalculated. */
    ExamResultSheetResponse getResultSheet(UUID examId, String authorizationHeader);
}