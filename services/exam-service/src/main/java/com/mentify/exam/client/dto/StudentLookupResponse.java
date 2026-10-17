package com.mentify.exam.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class StudentLookupResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String studentNumber;
}
