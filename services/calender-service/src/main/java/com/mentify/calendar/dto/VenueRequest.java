package com.mentify.calendar.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VenueRequest {
    @NotBlank private String name;
    @NotBlank private String location;
    @Min(1) private int capacity;
}
