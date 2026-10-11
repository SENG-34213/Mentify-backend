package com.mentify.calendar.repository;

import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarSourceType;
import com.mentify.calendar.enums.CalendarVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    Optional<CalendarEvent> findBySourceTypeAndSourceId(CalendarSourceType sourceType, UUID sourceId);

    @Query("""
            select event from CalendarEvent event
            where event.startDateTime <= :rangeEnd
              and coalesce(event.endDateTime, event.startDateTime) >= :rangeStart
              and (:courseId is null or event.courseId = :courseId)
              and (:createdBy is null or event.createdBy = :createdBy)
              and (:eventType is null or event.eventType = :eventType)
              and (:visibility is null or event.visibility = :visibility)
              and (:sourceType is null or event.sourceType = :sourceType)
              and (:status is null or event.status = :status)
            order by event.startDateTime asc, event.id asc
            """)
    List<CalendarEvent> findOverlapping(
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("courseId") UUID courseId,
            @Param("createdBy") UUID createdBy,
            @Param("eventType") CalendarEventType eventType,
            @Param("visibility") CalendarVisibility visibility,
            @Param("sourceType") CalendarSourceType sourceType,
            @Param("status") CalendarEventStatus status
    );

    List<CalendarEvent> findAllByOrderByStartDateTimeAscIdAsc();
}
