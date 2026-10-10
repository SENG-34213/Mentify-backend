package com.mentify.communication.repository;

import com.mentify.communication.entity.MessageHiddenForUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageHiddenForUserRepository extends JpaRepository<MessageHiddenForUser, UUID> {

    Optional<MessageHiddenForUser> findByMessage_IdAndUserId(UUID messageId, UUID userId);
}
