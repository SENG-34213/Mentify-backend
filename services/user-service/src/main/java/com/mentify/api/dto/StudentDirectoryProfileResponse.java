package com.mentify.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class StudentDirectoryProfileResponse {
    UUID id;
    String name;
    String email;
    String studentNumber;
    String avatar;
    String program;
    String status;
}
