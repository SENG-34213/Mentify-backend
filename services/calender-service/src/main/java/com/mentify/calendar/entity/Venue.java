package com.mentify.calendar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "calendar_venues")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Venue {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @NotBlank @Column(nullable = false, unique = true, length = 150)
    private String name;
    @NotBlank @Column(nullable = false, length = 255)
    private String location;
    @Min(1) @Column(nullable = false)
    private int capacity;
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
