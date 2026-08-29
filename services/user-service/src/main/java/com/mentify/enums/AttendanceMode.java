package com.mentify.enums;

/**
 * Specifies the mode of attendance for a student.
 * This is a business-specific enum relevant to the student's profile.
 */
public enum AttendanceMode {
    /**
     * The student attends classes exclusively online.
     */
    ONLINE,

    /**
     * The student attends classes in person.
     */
    PHYSICAL,

    /**
     * The student has a mix of online and physical attendance.
     */
    HYBRID
}
