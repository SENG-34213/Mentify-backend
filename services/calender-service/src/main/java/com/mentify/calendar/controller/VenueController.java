package com.mentify.calendar.controller;

import com.mentify.calendar.dto.VenueRequest;
import com.mentify.calendar.dto.VenueResponse;
import com.mentify.calendar.entity.Venue;
import com.mentify.calendar.repository.VenueRepository;
import com.mentify.payload.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar/venues")
@RequiredArgsConstructor
public class VenueController {
    private final VenueRepository repository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> list() {
        List<VenueResponse> venues = repository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Venues fetched successfully", venues));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<VenueResponse>> create(@Valid @RequestBody VenueRequest request) {
        Venue venue = repository.save(Venue.builder().name(request.getName().trim())
                .location(request.getLocation().trim()).capacity(request.getCapacity()).active(true).build());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Venue created successfully", toResponse(venue)));
    }

    private VenueResponse toResponse(Venue venue) {
        return VenueResponse.builder().id(venue.getId()).name(venue.getName()).location(venue.getLocation())
                .capacity(venue.getCapacity()).active(venue.isActive()).build();
    }
}
