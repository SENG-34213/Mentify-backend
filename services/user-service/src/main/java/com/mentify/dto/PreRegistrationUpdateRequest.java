package com.mentify.dto;

import com.mentify.enums.PreRegistrationStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreRegistrationUpdateRequest {
    private PreRegistrationStatus status;
    private Boolean seen;

    @Size(max = 2000)
    private String adminRemark;
}