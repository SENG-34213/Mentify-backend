package com.mentify.calendar.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseLookupResponse {
    private UUID id;
    private UUID assignedTeacherId;
}
