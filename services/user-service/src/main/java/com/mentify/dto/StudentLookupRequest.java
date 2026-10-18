package com.mentify.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentLookupRequest {

    @NotEmpty(message = "At least one student ID is required")
    @Size(max = 500, message = "At most 500 student IDs can be looked up at once")
    private List<@NotNull UUID> studentIds;
}
