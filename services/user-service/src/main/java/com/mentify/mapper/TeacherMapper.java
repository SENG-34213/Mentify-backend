package com.mentify.mapper;

import com.mentify.dto.UserResponse;
import com.mentify.entity.User;
import org.springframework.stereotype.Component;

@Component
public class TeacherMapper {
    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName((user.getFirstName() + " " + user.getLastName()).trim())
                .teacherCode(user.getTeacherProfile() == null ? null : user.getTeacherProfile().getTeacherCode())
                .title(user.getTeacherProfile() == null ? null : user.getTeacherProfile().getAcademicTitle())
                .department(user.getTeacherProfile() == null ? null : user.getTeacherProfile().getDepartment())
                .officeLocation(user.getTeacherProfile() == null ? null : user.getTeacherProfile().getOfficeLocation())
                .officeHours(user.getTeacherProfile() == null ? null : user.getTeacherProfile().getOfficeHours())
                .staffMetadata(user.getTeacherProfile() == null || user.getTeacherProfile().getSpecializations() == null ? null : String.join(", ", user.getTeacherProfile().getSpecializations()))
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
