package com.mentify.calendar.dto;

import lombok.Builder;
import lombok.Value;
import java.util.UUID;

@Value @Builder
public class VenueResponse {
    UUID id;
    String name;
    String location;
    int capacity;
    boolean active;
}
