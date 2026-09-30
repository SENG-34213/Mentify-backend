package com.mentify.ai.entity;

import com.mentify.ai.enums.AiMessageRole;
import com.mentify.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "ai_messages",
        indexes = {
                @Index(name = "idx_ai_messages_conversation_created_at", columnList = "conversation_id,created_at"),
                @Index(name = "idx_ai_messages_conversation_client_message_id", columnList = "conversation_id,client_message_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ai_messages_conversation_client_message_id",
                        columnNames = {"conversation_id", "client_message_id"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AiConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiMessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "client_message_id", length = 100)
    private String clientMessageId;
}
