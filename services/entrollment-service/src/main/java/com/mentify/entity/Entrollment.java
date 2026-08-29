package com.mentify.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "entrollments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entrollment extends BaseEntity {

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID studentId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "entrollment_courses", joinColumns = @JoinColumn(name = "entrollment_id"))
    @Column(name = "course_id", nullable = false, columnDefinition = "uuid")
    @Builder.Default
    private Set<UUID> courseIds = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private LocalDate enrolledOn = LocalDate.now();
}

