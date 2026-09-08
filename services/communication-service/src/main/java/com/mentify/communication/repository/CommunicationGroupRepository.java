package com.mentify.communication.repository;

import com.mentify.communication.entity.CommunicationGroup;
import com.mentify.communication.enums.GroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommunicationGroupRepository extends JpaRepository<CommunicationGroup, UUID> {

    boolean existsByCourseId(UUID courseId);

    Optional<CommunicationGroup> findByIdAndStatus(UUID id, GroupStatus status);

    Optional<CommunicationGroup> findByCourseIdAndStatus(UUID courseId, GroupStatus status);
}
