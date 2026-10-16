package com.mentify.communication.repository;

import com.mentify.communication.entity.Message;
import com.mentify.communication.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByGroup_IdAndStatus(UUID groupId, MessageStatus status, Pageable pageable);

        Optional<Message> findByIdAndGroup_Id(UUID id, UUID groupId);

        @Query("""
                        SELECT m
                        FROM Message m
                        WHERE m.group.id = :groupId
                            AND m.status IN :statuses
                            AND NOT EXISTS (
                                    SELECT 1
                                    FROM MessageHiddenForUser hidden
                                    WHERE hidden.message.id = m.id
                                        AND hidden.userId = :userId
                                        AND hidden.isActive = true
                            )
                        """)
        Page<Message> findVisibleMessagesByGroupAndStatuses(
                        @Param("groupId") UUID groupId,
                        @Param("statuses") Collection<MessageStatus> statuses,
                        @Param("userId") UUID userId,
                        Pageable pageable
        );
}
