package com.mentify.calendar.repository;

import com.mentify.calendar.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
    List<Venue> findAllByActiveTrueOrderByNameAsc();
}
