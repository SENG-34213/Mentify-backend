package com.mentify.communication.repository;

import com.mentify.communication.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {

    List<MessageAttachment> findByMessage_IdOrderByCreatedAtAsc(UUID messageId);
}
