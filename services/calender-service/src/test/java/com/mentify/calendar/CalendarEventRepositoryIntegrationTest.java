package com.mentify.calendar;

import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.enums.CalendarEventType;
import com.mentify.calendar.enums.CalendarSourceType;
import com.mentify.calendar.enums.CalendarTargetRole;
import com.mentify.calendar.enums.CalendarVisibility;
import com.mentify.calendar.repository.CalendarEventRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@ActiveProfiles("test")
class CalendarEventRepositoryIntegrationTest {

    @Autowired
    private CalendarEventRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanEvents() {
        repository.deleteAll();
        repository.flush();
    }

    @Test
    void persistsAllFieldsAndGeneratedAuditValues() {
        UUID creator = UUID.randomUUID();
        UUID course = UUID.randomUUID();
        CalendarEvent event = event("Persistence", LocalDateTime.of(2026, 10, 12, 9, 0));
        event.setDescription("Description");
        event.setEventType(CalendarEventType.CLASS);
        event.setCourseId(course);
        event.setCreatedBy(creator);
        event.setVisibility(CalendarVisibility.COURSE);
        event.setTargetRole(CalendarTargetRole.STUDENT);
        event.setLocation("Room 1");

        CalendarEvent saved = repository.saveAndFlush(event);
        CalendarEvent loaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getTitle()).isEqualTo("Persistence");
        assertThat(loaded.getDescription()).isEqualTo("Description");
        assertThat(loaded.getEventType()).isEqualTo(CalendarEventType.CLASS);
        assertThat(loaded.getCourseId()).isEqualTo(course);
        assertThat(loaded.getCreatedBy()).isEqualTo(creator);
        assertThat(loaded.getVisibility()).isEqualTo(CalendarVisibility.COURSE);
        assertThat(loaded.getTargetRole()).isEqualTo(CalendarTargetRole.STUDENT);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void allowsMultipleManualEventsWithNullSourceIds() {
        repository.saveAndFlush(event("Manual 1", LocalDateTime.of(2026, 10, 12, 9, 0)));
        repository.saveAndFlush(event("Manual 2", LocalDateTime.of(2026, 10, 12, 10, 0)));
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void enforcesUniqueExternalSourcePair() {
        UUID sourceId = UUID.randomUUID();
        CalendarEvent first = event("First", LocalDateTime.of(2026, 10, 12, 9, 0));
        first.setSourceType(CalendarSourceType.EXAM);
        first.setSourceId(sourceId);
        first.setEventType(CalendarEventType.EXAM);
        repository.saveAndFlush(first);

        CalendarEvent duplicate = event("Duplicate", LocalDateTime.of(2026, 10, 12, 10, 0));
        duplicate.setSourceType(CalendarSourceType.EXAM);
        duplicate.setSourceId(sourceId);
        duplicate.setEventType(CalendarEventType.EXAM);

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsSameSourceIdForDifferentSourceTypes() {
        UUID sourceId = UUID.randomUUID();
        CalendarEvent exam = event("Exam", LocalDateTime.of(2026, 10, 12, 9, 0));
        exam.setSourceType(CalendarSourceType.EXAM);
        exam.setSourceId(sourceId);
        exam.setEventType(CalendarEventType.EXAM);
        CalendarEvent assignment = event("Assignment", LocalDateTime.of(2026, 10, 12, 10, 0));
        assignment.setSourceType(CalendarSourceType.ASSIGNMENT);
        assignment.setSourceId(sourceId);
        assignment.setEventType(CalendarEventType.ASSIGNMENT);

        repository.saveAll(List.of(exam, assignment));
        repository.flush();
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void preservesCreatorAndCreationTimeWhenUpdatedOrCancelled() throws InterruptedException {
        UUID creator = UUID.randomUUID();
        CalendarEvent event = event("Original", LocalDateTime.of(2026, 10, 12, 9, 0));
        event.setCreatedBy(creator);
        CalendarEvent saved = repository.saveAndFlush(event);
        LocalDateTime createdAt = saved.getCreatedAt();
        LocalDateTime updatedAt = saved.getUpdatedAt();

        Thread.sleep(5);
        saved.setTitle("Updated");
        saved.setStatus(CalendarEventStatus.CANCELLED);
        repository.saveAndFlush(saved);

        CalendarEvent loaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getCreatedBy()).isEqualTo(creator);
        assertThat(loaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(loaded.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
        assertThat(loaded.getStatus()).isEqualTo(CalendarEventStatus.CANCELLED);
    }

    @Test
    void returnsInclusiveOverlapsWithOptionalFiltersAndDeterministicChronology() {
        UUID course = UUID.randomUUID();
        UUID creator = UUID.randomUUID();
        CalendarEvent beforeRange = event("Before", LocalDateTime.of(2026, 10, 12, 8, 0));
        beforeRange.setEndDateTime(LocalDateTime.of(2026, 10, 12, 9, 30));
        beforeRange.setCourseId(course);
        beforeRange.setCreatedBy(creator);
        CalendarEvent atStart = event("At start", LocalDateTime.of(2026, 10, 12, 10, 0));
        atStart.setCourseId(course);
        atStart.setCreatedBy(creator);
        CalendarEvent excludedCourse = event("Other course", LocalDateTime.of(2026, 10, 12, 10, 30));
        excludedCourse.setCourseId(UUID.randomUUID());
        CalendarEvent afterRange = event("After", LocalDateTime.of(2026, 10, 12, 12, 0));
        afterRange.setCourseId(course);

        repository.saveAll(List.of(beforeRange, atStart, excludedCourse, afterRange));
        repository.flush();

        List<CalendarEvent> result = repository.findOverlapping(
                LocalDateTime.of(2026, 10, 12, 9, 30),
                LocalDateTime.of(2026, 10, 12, 11, 0),
                course, null, null, null, null, CalendarEventStatus.SCHEDULED);

        assertThat(result).extracting(CalendarEvent::getTitle)
                .containsExactly("Before", "At start");
    }

    @Test
    void databaseChecksRejectUnsupportedAndInvalidConditionalValues() {
        assertThatThrownBy(() -> jdbcTemplate.execute("insert into calendar_events " +
                "(id,title,event_type,start_date_time,end_date_time,all_day,source_type,created_by,visibility,status,created_at,updated_at) " +
                "values ('" + UUID.randomUUID() + "','Title','UNSUPPORTED','2026-10-12 09:00:00'," +
                "'2026-10-12 10:00:00',false,'CUSTOM','" + UUID.randomUUID() + "','PRIVATE','SCHEDULED'," +
                "current_timestamp,current_timestamp)"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void migrationIsRepeatableAndRecordedOnce() {
        flyway.migrate();
        flyway.migrate();
        Integer v2Count = jdbcTemplate.queryForObject(
                "select count(*) from \"flyway_schema_history\" where \"version\" = '2'", Integer.class);
        assertThat(v2Count).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from information_schema.tables " +
                "where upper(table_name) = 'CALENDAR_EVENTS'", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentExternalCreatesAllowExactlyOneSourcePair() throws Exception {
        UUID sourceId = UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            results.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    transaction.executeWithoutResult(status -> {
                        CalendarEvent event = event("Concurrent", LocalDateTime.of(2026, 10, 12, 9, 0));
                        event.setSourceType(CalendarSourceType.EXAM);
                        event.setSourceId(sourceId);
                        event.setEventType(CalendarEventType.EXAM);
                        repository.saveAndFlush(event);
                    });
                    return true;
                } catch (DataAccessException exception) {
                    return false;
                }
            }));
        }
        ready.await();
        start.countDown();

        assertThat(results.get(0).get()).isNotEqualTo(results.get(1).get());
        executor.shutdownNow();
        assertThat(repository.findBySourceTypeAndSourceId(CalendarSourceType.EXAM, sourceId)).isPresent();
    }

    private static CalendarEvent event(String title, LocalDateTime start) {
        return CalendarEvent.builder()
                .title(title)
                .eventType(CalendarEventType.CUSTOM)
                .startDateTime(start)
                .endDateTime(start.plusHours(1))
                .allDay(false)
                .sourceType(CalendarSourceType.CUSTOM)
                .createdBy(UUID.randomUUID())
                .visibility(CalendarVisibility.PRIVATE)
                .status(CalendarEventStatus.SCHEDULED)
                .build();
    }
}
