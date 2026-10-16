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
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
