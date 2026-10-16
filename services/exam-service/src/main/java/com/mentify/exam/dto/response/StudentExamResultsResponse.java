package com.mentify.exam.dto.response;

import lombok.Value;

import java.util.List;

@Value
public class StudentExamResultsResponse {
    List<StudentExamResultResponse> results;
}