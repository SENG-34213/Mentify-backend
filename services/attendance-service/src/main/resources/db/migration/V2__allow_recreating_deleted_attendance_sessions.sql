ALTER TABLE attendance_sessions
    DROP CONSTRAINT IF EXISTS uk_attendance_session_course_date_start;

CREATE UNIQUE INDEX IF NOT EXISTS uk_attendance_session_course_date_start
    ON attendance_sessions (course_id, attendance_date, start_time)
    WHERE is_active = TRUE;
