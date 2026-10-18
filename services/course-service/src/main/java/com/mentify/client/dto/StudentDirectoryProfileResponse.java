package com.mentify.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentDirectoryProfileResponse {
    private UUID id;
    private String name;
    private String email;
    private String studentNumber;
    private String avatar;
    private String program;
    private String status;
}
