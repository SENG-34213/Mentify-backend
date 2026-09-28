package com.mentify.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLookupResponse {
    private UUID id;
    private String keycloakUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
    private String accountStatus;
    private StudentProfileResponse studentProfile;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StudentProfileResponse {
        private UUID id;
        private String studentId;
        private String grade;
        private String attendanceMode;
    }
}
