package com.mentify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PreRegistrationRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @NotBlank
    @Pattern(regexp = "^[+()\\d\\s.-]{7,25}$")
    private String phone;

    @NotBlank
    @Pattern(regexp = "^(student|parent)$")
    private String role;

    @NotBlank
    @Size(max = 40)
    private String district;

    @NotBlank
    @Size(max = 100)
    private String city;

    @NotBlank
    @Pattern(regexp = "^(online|onsite|either)$")
    private String classFormat;

    @NotBlank
    @Pattern(regexp = "^(1[0-1]|[1-9]|al)$")
    private String grade;

    @NotEmpty
    @Size(max = 20)
    private List<@NotBlank @Size(max = 80) String> subjects;

    @NotBlank
    @Pattern(regexp = "^(whatsapp|phone|email)$")
    private String contactMethod;

    @NotNull
    private Boolean consent;
}