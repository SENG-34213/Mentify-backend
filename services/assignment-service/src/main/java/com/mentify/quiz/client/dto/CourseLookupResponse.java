package com.mentify.quiz.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CourseLookupResponse {

    private UUID id;
    private UUID assignedTeacherId;
    private boolean published;
    private boolean visible;
}
