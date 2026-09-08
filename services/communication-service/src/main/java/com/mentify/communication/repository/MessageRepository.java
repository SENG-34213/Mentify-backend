package com.mentify.communication.repository;

import com.mentify.communication.entity.Message;
import com.mentify.communication.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByGroup_IdAndStatus(UUID groupId, MessageStatus status, Pageable pageable);
}
