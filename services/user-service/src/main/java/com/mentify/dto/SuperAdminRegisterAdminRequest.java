package com.mentify.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminRegisterAdminRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Pattern(regexp = "^(?:\\+94|0)[7][0-9]{8}$", message = "Invalid Sri Lankan phone number")
    private String phoneNumber;

    @Valid
    @NotNull(message = "Admin profile details are required")
    private AdminProfileRequest adminProfile;

    @Valid
    @NotNull(message = "Address details are required")
    private AddressDto address;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminProfileRequest {

        @Past(message = "Date of birth must be in the past")
        private LocalDate dateOfBirth;

        private String nic;
    }
}
