package com.mentify.exam.enums;

/** JWT roles supported by the Exam Service (V1). */
public enum ExamRole {
    SUPER_ADMIN,
    ADMIN,
    TEACHER,
    STUDENT;

    public String authority() {
        return "ROLE_" + name();
    }
}
