package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TeacherSearchResponse {
    List<TeacherSearchResultResponse> results;
    int total;
}
