package com.mentify.calendar.service;

import com.mentify.calendar.entity.CalendarEvent;
import com.mentify.calendar.enums.CalendarEventStatus;
import com.mentify.calendar.exception.CalendarDomainException;
import com.mentify.calendar.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarVenueAvailabilityService {

    private final CalendarEventRepository repository;

    public void assertAvailable(CalendarEvent candidate, UUID excludedEventId) {
        String venue = normalizeVenue(candidate.getLocation());
        if (venue.isBlank() || isNonExclusiveLocation(venue)) {
            return;
        }

        TimeRange candidateRange = rangeOf(candidate);
        boolean conflict = repository.findVenueBookings(venue, CalendarEventStatus.SCHEDULED).stream()
                .filter(existing -> !existing.getId().equals(excludedEventId))
                .filter(existing -> venue.equals(normalizeVenue(existing.getLocation())))
                .anyMatch(existing -> overlaps(candidateRange, rangeOf(existing)));
        if (conflict) {
            throw new CalendarDomainException(HttpStatus.CONFLICT,
                    "That venue is already booked during this time. Choose another time or venue.");
        }
    }

    private TimeRange rangeOf(CalendarEvent event) {
        LocalDateTime start = event.isAllDay()
                ? event.getStartDateTime().toLocalDate().atStartOfDay()
                : event.getStartDateTime();
        LocalDateTime end = event.getEndDateTime();
        if (event.isAllDay()) {
            if (end == null || !end.isAfter(start)) {
                end = start.toLocalDate().plusDays(1).atStartOfDay();
            } else if (!end.toLocalTime().equals(LocalTime.MIDNIGHT)) {
                end = end.toLocalDate().plusDays(1).atStartOfDay();
            }
        } else if (end == null) {
            end = start;
        }
        return new TimeRange(start, end);
    }

    private boolean overlaps(TimeRange left, TimeRange right) {
        return left.start().isBefore(right.end()) && right.start().isBefore(left.end());
    }

    private String normalizeVenue(String location) {
        if (location == null) {
            return "";
        }
        return Normalizer.normalize(location.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private boolean isNonExclusiveLocation(String venue) {
        return venue.equals("campus wide")
                || venue.equals("tit lms submission portal")
                || venue.equals("not specified")
                || venue.equals("online")
                || venue.equals("virtual");
    }

    private record TimeRange(LocalDateTime start, LocalDateTime end) {
    }
}
