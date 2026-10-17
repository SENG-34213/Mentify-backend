package com.mentify.dto;

import com.mentify.entity.User;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class StudentLookupResponse {
    UUID id;
    String firstName;
    String lastName;
    String email;
    String studentNumber;

    public static StudentLookupResponse from(User user) {
        return StudentLookupResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .studentNumber(user.getStudentProfile() == null ? null : user.getStudentProfile().getStudentId())
                .build();
    }
}
