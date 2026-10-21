package com.mentify.quiz.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CourseLookupResponse {

    private UUID id;
    private String courseName;
    private UUID assignedTeacherId;
    private boolean published;
    private boolean visible;
}
