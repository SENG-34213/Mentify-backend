package com.mentify.ai.repository;

import com.mentify.ai.entity.AiMessage;
import com.mentify.ai.enums.AiMessageRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {

    Page<AiMessage> findByConversation_Id(UUID conversationId, Pageable pageable);

    List<AiMessage> findByConversation_IdOrderByCreatedAtAsc(UUID conversationId);

    List<AiMessage> findByConversation_IdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    Optional<AiMessage> findFirstByConversation_IdAndRoleAndClientMessageId(
            UUID conversationId,
            AiMessageRole role,
            String clientMessageId
    );

    Optional<AiMessage> findFirstByConversation_IdAndRoleAndCreatedAtAfterOrderByCreatedAtAsc(
            UUID conversationId,
            AiMessageRole role,
            LocalDateTime createdAt
    );
}
