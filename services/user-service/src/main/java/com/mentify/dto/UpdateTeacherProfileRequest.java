package com.mentify.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateTeacherProfileRequest {
    @Size(max = 20, message = "At most 20 specializations are allowed")
    private List<@Size(max = 100, message = "Specialization must not exceed 100 characters") String> specializations;
    @Pattern(regexp = "^(?:\\+94|0)[7][0-9]{8}$", message = "Invalid phone number")
    private String phoneNumber;
}
