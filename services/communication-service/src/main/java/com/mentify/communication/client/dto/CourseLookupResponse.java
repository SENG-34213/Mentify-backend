package com.mentify.communication.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CourseLookupResponse {

    private UUID id;
    private String courseName;
    private UUID assignedTeacherId;
}
