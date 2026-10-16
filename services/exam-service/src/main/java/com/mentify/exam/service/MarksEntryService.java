package com.mentify.exam.service;

import com.mentify.exam.dto.request.BatchMarksEntryRequest;
import com.mentify.exam.dto.response.MarksEntryResponse;

import java.util.UUID;

public interface MarksEntryService {

    /**
     * Records attendance and marks for already-initialized results of a MARKING exam.
     * The whole batch is validated first and persisted atomically; PASS/FAIL is computed here.
     */
    MarksEntryResponse enterMarks(UUID examId, BatchMarksEntryRequest request, String authorizationHeader);
}