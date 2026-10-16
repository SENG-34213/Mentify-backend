package com.mentify.communication.repository;

import com.mentify.communication.entity.CommunicationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommunicationSettingsRepository extends JpaRepository<CommunicationSettings, UUID> {

    Optional<CommunicationSettings> findByUserId(UUID userId);
}
