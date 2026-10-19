package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class TeacherSearchResultResponse {
    UUID id;
    String type;
    String title;
    String subtitle;
    UUID courseId;
    String navigationTarget;
}
