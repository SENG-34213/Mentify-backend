package com.mentify.dto;

import com.mentify.enums.AttendanceMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRegistrationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /** Accepted for backward-compatible payloads; registration now generates a temporary credential. */
    @Deprecated
    @ToString.Exclude
    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "School name is required")
    @Size(max = 150, message = "School name must not exceed 150 characters")
    private String schoolName;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(?:\\+94|0)[7][0-9]{8}$", message = "Invalid Sri Lankan phone number")
    private String phoneNumber;

    @NotBlank(message = "Guardian name is required")
    private String guardianName;

    @NotBlank(message = "Guardian phone is required")
    @Pattern(regexp = "^(?:\\+94|0)[7][0-9]{8}$", message = "Invalid Sri Lankan phone number")
    private String guardianPhone;

    @NotNull(message = "Attendance mode is required")
    private AttendanceMode attendanceMode;

    @Valid // Important: This triggers validation on the nested object
    @NotNull(message = "Address details are required")
    private AddressDto address;
}
