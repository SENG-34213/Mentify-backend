package com.mentify.dto;

import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private Role role;
    private AccountStatus accountStatus;
    private boolean emailVerified;
    private LocalDateTime createdAt;
}
