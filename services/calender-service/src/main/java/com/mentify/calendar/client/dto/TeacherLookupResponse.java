package com.mentify.calendar.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class TeacherLookupResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String title;
    private String department;
}
