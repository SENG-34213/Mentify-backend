package com.mentify.communication.exception;

import java.util.UUID;

public class TeacherNotAssignedToCourseException extends RuntimeException {

    public TeacherNotAssignedToCourseException(UUID teacherId, UUID courseId) {
        super("Teacher '" + teacherId + "' is not assigned to course '" + courseId + "'");
    }
}
