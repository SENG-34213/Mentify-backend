package com.mentify.exam.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Minimal view of a Course Service course; only the fields the Exam Service needs. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseLookupResponse {
    private UUID id;
    private String courseName;
    private UUID assignedTeacherId;
}
