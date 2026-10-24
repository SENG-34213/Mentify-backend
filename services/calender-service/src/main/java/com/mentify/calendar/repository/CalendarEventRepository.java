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
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    Optional<CalendarEvent> findBySourceTypeAndSourceId(CalendarSourceType sourceType, UUID sourceId);

    @Query("""
            select event from CalendarEvent event
            where event.status = :status
              and event.location is not null
              and lower(trim(event.location)) = :normalizedLocation
            order by event.startDateTime asc, event.id asc
            """)
    List<CalendarEvent> findVenueBookings(
            @Param("normalizedLocation") String normalizedLocation,
            @Param("status") CalendarEventStatus status
    );

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

    @Query("""
            select event from CalendarEvent event
            where event.startDateTime < :rangeEnd
              and coalesce(event.endDateTime, event.startDateTime) > :rangeStart
              and (:courseId is null or event.courseId = :courseId)
              and (:eventType is null or event.eventType = :eventType)
              and (:status is null or event.status = :status)
              and (event.visibility = com.mentify.calendar.enums.CalendarVisibility.GLOBAL
                   or (event.visibility = com.mentify.calendar.enums.CalendarVisibility.PRIVATE
                       and event.createdBy = :userId)
                   or (event.visibility = com.mentify.calendar.enums.CalendarVisibility.ROLE
                       and event.targetRole in :roles)
                   or (event.visibility = com.mentify.calendar.enums.CalendarVisibility.COURSE
                       and event.courseId in :courseIds))
            order by event.startDateTime asc, event.id asc
            """)
    List<CalendarEvent> findVisibleOverlapping(
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("courseId") UUID courseId,
            @Param("eventType") CalendarEventType eventType,
            @Param("status") CalendarEventStatus status,
            @Param("userId") UUID userId,
            @Param("roles") Set<com.mentify.calendar.enums.CalendarTargetRole> roles,
            @Param("courseIds") Set<UUID> courseIds
    );

    @Query("""
            select event from CalendarEvent event
            where event.startDateTime < :rangeEnd
              and coalesce(event.endDateTime, event.startDateTime) > :rangeStart
              and (:courseId is null or event.courseId = :courseId)
              and (:eventType is null or event.eventType = :eventType)
              and (:status is null or event.status = :status)
            order by event.startDateTime asc, event.id asc
            """)
    List<CalendarEvent> findAdministrativeOverlapping(
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("courseId") UUID courseId,
            @Param("eventType") CalendarEventType eventType,
            @Param("status") CalendarEventStatus status
    );

    @Query("""
            select event from CalendarEvent event
            where event.startDateTime >= :from
              and (:courseId is null or event.courseId = :courseId)
              and (:eventType is null or event.eventType = :eventType)
              and (:status is null or event.status = :status)
              and (event.visibility = com.mentify.calendar.enums.CalendarVisibility.GLOBAL
                   or (event.visibility = com.mentify.calendar.enums.CalendarVisibility.PRIVATE
                       and event.createdBy = :userId)
                   or (event.visibility = com.mentify.calendar.enums.CalendarVisibility.ROLE
                       and event.targetRole in :roles)
                   or (event.visibility = com.mentify.calendar.enums.CalendarVisibility.COURSE
                       and event.courseId in :courseIds))
            order by event.startDateTime asc, event.id asc
            """)
    List<CalendarEvent> findVisibleUpcoming(
            @Param("from") LocalDateTime from,
            @Param("courseId") UUID courseId,
            @Param("eventType") CalendarEventType eventType,
            @Param("status") CalendarEventStatus status,
            @Param("userId") UUID userId,
            @Param("roles") Set<com.mentify.calendar.enums.CalendarTargetRole> roles,
            @Param("courseIds") Set<UUID> courseIds
    );

    @Query("""
            select event from CalendarEvent event
            where event.startDateTime >= :from
              and (:courseId is null or event.courseId = :courseId)
              and (:eventType is null or event.eventType = :eventType)
              and (:status is null or event.status = :status)
            order by event.startDateTime asc, event.id asc
            """)
    List<CalendarEvent> findAdministrativeUpcoming(
            @Param("from") LocalDateTime from,
            @Param("courseId") UUID courseId,
            @Param("eventType") CalendarEventType eventType,
            @Param("status") CalendarEventStatus status
    );

    List<CalendarEvent> findAllByOrderByStartDateTimeAscIdAsc();
}
