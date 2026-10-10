package com.mentify.ai.entity;

import com.mentify.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "ai_conversations",
        indexes = {
                @Index(name = "idx_ai_conversations_user_created_at", columnList = "user_id,created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConversation extends BaseEntity {

    private static final String DEFAULT_TITLE = "New conversation";

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false, length = 150, columnDefinition = "varchar(150) default 'New conversation'")
    @Builder.Default
    private String title = DEFAULT_TITLE;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<AiMessage> messages = new ArrayList<>();

    public void addMessage(AiMessage message) {
        messages.add(message);
        message.setConversation(this);
    }

    public void setTitle(String title) {
        this.title = title == null || title.isBlank() ? DEFAULT_TITLE : title.trim();
    }
}
