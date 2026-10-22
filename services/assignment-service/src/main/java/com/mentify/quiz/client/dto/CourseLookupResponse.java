package com.mentify.quiz.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CourseLookupResponse {

    private UUID id;
    private String courseName;
    private String courseCode;
    private String level;
    private String semester;
    private String academicYear;
    private Integer maxCapacity;
    private UUID assignedTeacherId;
    private boolean published;
    private boolean visible;
}
