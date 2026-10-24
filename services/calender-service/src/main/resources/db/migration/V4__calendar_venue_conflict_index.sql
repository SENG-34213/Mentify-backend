CREATE INDEX idx_calendar_events_active_venue_time
    ON calendar_events (status, start_date_time);
