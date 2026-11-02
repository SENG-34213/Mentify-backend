package com.mentify.entity;

import com.mentify.enums.PreRegistrationStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pre_registrations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 25)
    private String phone;

    @Column(nullable = false, length = 10)
    private String role;

    @Column(nullable = false, length = 40)
    private String district;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "class_format", nullable = false, length = 10)
    private String classFormat;

    @Column(nullable = false, length = 2)
    private String grade;

    @ElementCollection
    @CollectionTable(name = "pre_registration_subjects", joinColumns = @JoinColumn(name = "pre_registration_id"))
    @OrderColumn(name = "subject_order")
    @Column(name = "subject", nullable = false, length = 80)
    @Builder.Default
    private List<String> subjects = new ArrayList<>();

    @Column(name = "contact_method", nullable = false, length = 10)
    private String contactMethod;

    @Column(name = "consent_confirmed", nullable = false)
    private boolean consent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PreRegistrationStatus status = PreRegistrationStatus.NEW;

    @Column(nullable = false)
    @Builder.Default
    private boolean seen = false;

    @Column(name = "admin_remark", length = 2000)
    private String adminRemark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}