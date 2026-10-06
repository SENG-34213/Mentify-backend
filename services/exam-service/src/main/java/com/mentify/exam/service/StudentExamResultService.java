package com.mentify.exam.service;

import com.mentify.exam.dto.response.StudentExamResultsResponse;

public interface StudentExamResultService {

    /** Completed-exam results of the JWT-authenticated student only. */
    StudentExamResultsResponse getMyResults();
}