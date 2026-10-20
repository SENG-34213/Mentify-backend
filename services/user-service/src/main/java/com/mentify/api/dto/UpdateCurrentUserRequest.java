package com.mentify.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalTime;

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
    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;
    @Size(max = 150, message = "School name must not exceed 150 characters")
    private String schoolName;
    @Valid
    private AddressDto address;
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean dailyDigestEnabled;
    private LocalTime dailyDigestTime;
}
