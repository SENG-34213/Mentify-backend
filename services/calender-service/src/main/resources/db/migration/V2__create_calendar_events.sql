CREATE TABLE calendar_events (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    event_type VARCHAR(30) NOT NULL,
    start_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_date_time TIMESTAMP WITHOUT TIME ZONE,
    all_day BOOLEAN NOT NULL,
    course_id UUID,
    source_type VARCHAR(30) NOT NULL,
    source_id UUID,
    created_by UUID NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    target_role VARCHAR(30),
    status VARCHAR(20) NOT NULL,
    location VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    CONSTRAINT uq_calendar_events_source_pair UNIQUE (source_type, source_id),
    CONSTRAINT ck_calendar_events_end_after_start
        CHECK (end_date_time IS NULL OR end_date_time >= start_date_time),
    CONSTRAINT ck_calendar_events_timed_end
        CHECK (all_day OR end_date_time IS NOT NULL),
    CONSTRAINT ck_calendar_events_course_visibility
        CHECK (visibility <> 'COURSE' OR course_id IS NOT NULL),
    CONSTRAINT ck_calendar_events_role_visibility
        CHECK (visibility <> 'ROLE' OR target_role IS NOT NULL),
    CONSTRAINT ck_calendar_events_source_reference
        CHECK (source_type IN ('CUSTOM', 'ADMIN') OR source_id IS NOT NULL),
    CONSTRAINT ck_calendar_events_event_type
        CHECK (event_type IN ('CLASS', 'ASSIGNMENT', 'QUIZ', 'EXAM', 'MEETING', 'HOLIDAY', 'ADMIN', 'CUSTOM', 'ATTENDANCE')),
    CONSTRAINT ck_calendar_events_visibility
        CHECK (visibility IN ('PRIVATE', 'COURSE', 'ROLE', 'GLOBAL')),
    CONSTRAINT ck_calendar_events_source_type
        CHECK (source_type IN ('EXAM', 'ASSIGNMENT', 'QUIZ', 'COURSE_CLASS', 'ATTENDANCE', 'COMMUNICATION', 'CUSTOM', 'ADMIN')),
    CONSTRAINT ck_calendar_events_status
        CHECK (status IN ('SCHEDULED', 'CANCELLED')),
    CONSTRAINT ck_calendar_events_target_role
        CHECK (target_role IS NULL OR target_role IN ('STUDENT', 'TEACHER', 'ADMIN', 'SUPER_ADMIN'))
);

CREATE INDEX idx_calendar_events_start_date_time ON calendar_events (start_date_time);
CREATE INDEX idx_calendar_events_end_date_time ON calendar_events (end_date_time);
CREATE INDEX idx_calendar_events_course_id ON calendar_events (course_id);
CREATE INDEX idx_calendar_events_created_by ON calendar_events (created_by);
CREATE INDEX idx_calendar_events_event_type ON calendar_events (event_type);
CREATE INDEX idx_calendar_events_status ON calendar_events (status);
CREATE INDEX idx_calendar_events_visibility ON calendar_events (visibility);
