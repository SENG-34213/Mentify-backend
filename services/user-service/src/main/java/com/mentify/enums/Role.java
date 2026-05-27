package com.mentify.enums;

/**
 * Defines the primary roles within the LMS.
 * A user has one primary role that governs their basic access level.
 */
public enum Role {
    /**
     * A student user, primarily consuming content.
     */
    STUDENT,

    /**
     * A teacher user, primarily creating and managing content and students.
     */
    TEACHER,

    /**
     * An administrative user with overarching permissions for day-to-day management.
     */
    ADMIN,

    /**
     * A super-administrator with the highest level of permissions, capable of managing
     * the entire system, including other admins.
     */
    SUPER_ADMIN,

    /**
     * A support role with specific, often read-only, permissions to assist users
     * and troubleshoot issues.
     */
    SUPPORT
}
