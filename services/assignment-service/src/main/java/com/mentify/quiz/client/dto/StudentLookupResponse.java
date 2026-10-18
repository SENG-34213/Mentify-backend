package com.mentify.quiz.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class StudentLookupResponse {
    private UUID studentId;
    private String firstName;
    private String lastName;
}
