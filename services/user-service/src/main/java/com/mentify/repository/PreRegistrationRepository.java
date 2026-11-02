package com.mentify.repository;

import com.mentify.entity.PreRegistration;
import com.mentify.enums.PreRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PreRegistrationRepository extends JpaRepository<PreRegistration, UUID> {
    @Query("""
            SELECT registration FROM PreRegistration registration
            WHERE (:status IS NULL OR registration.status = :status)
              AND (:seen IS NULL OR registration.seen = :seen)
            """)
    Page<PreRegistration> findByFilters(
            @Param("status") PreRegistrationStatus status,
            @Param("seen") Boolean seen,
            Pageable pageable);
}