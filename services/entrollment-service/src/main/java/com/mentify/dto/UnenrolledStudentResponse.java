package com.mentify.dto;

import com.mentify.client.dto.UserLookupResponse;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UnenrolledStudentResponse {
    private UUID id;
    private String keycloakUserId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String accountStatus;
    private String studentId;
    private String grade;

    public static UnenrolledStudentResponse from(UserLookupResponse user) {
        UserLookupResponse.StudentProfileResponse profile = user.getStudentProfile();
        return UnenrolledStudentResponse.builder()
                .id(user.getId())
                .keycloakUserId(user.getKeycloakUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .accountStatus(user.getAccountStatus())
                .studentId(profile != null ? profile.getStudentId() : null)
                .grade(profile != null ? profile.getGrade() : null)
                .build();
    }
}
