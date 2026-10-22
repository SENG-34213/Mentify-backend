package com.mentify.repository;

import java.util.UUID;

/** Lightweight grouped count returned by course summary queries. */
public interface CourseCountProjection {
    UUID getCourseId();

    Long getCount();
}
