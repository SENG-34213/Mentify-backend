package com.mentify.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCurrentUserRequest {
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    @Pattern(regexp = "^(?:\\+94|0)[7][0-9]{8}$", message = "Invalid phone number")
    private String phoneNumber;
    private Boolean emailNotifications;
    private Boolean pushNotifications;
}
