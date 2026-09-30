package com.mentify.ai.repository;

import com.mentify.ai.entity.AiConversation;
import com.mentify.ai.entity.AiMessage;
import com.mentify.ai.enums.AiMessageRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AiConversationPersistenceTest {

    @Autowired
    private AiConversationRepository conversationRepository;

    @Autowired
    private AiMessageRepository messageRepository;

    @Test
    void conversationCanBePersistedWithAuthenticatedUserOwnership() {
        UUID userId = UUID.randomUUID();

        AiConversation savedConversation = conversationRepository.save(AiConversation.builder()
                .userId(userId)
                .build());

        assertThat(savedConversation.getId()).isNotNull();
        assertThat(savedConversation.getUserId()).isEqualTo(userId);
        assertThat(savedConversation.getCreatedAt()).isNotNull();
        assertThat(conversationRepository.existsByIdAndUserId(savedConversation.getId(), userId)).isTrue();
    }

    @Test
    void oneUserCanHaveMultipleConversations() {
        UUID userId = UUID.randomUUID();

        conversationRepository.save(AiConversation.builder().userId(userId).build());
        conversationRepository.save(AiConversation.builder().userId(userId).build());
        conversationRepository.save(AiConversation.builder().userId(UUID.randomUUID()).build());

        assertThat(conversationRepository.findByUserId(userId, PageRequest.of(0, 10)).getContent())
                .hasSize(2)
                .allMatch(conversation -> userId.equals(conversation.getUserId()));
    }

    @Test
    void oneConversationCanHaveMultipleMessagesWithSupportedRoles() {
        UUID userId = UUID.randomUUID();
        AiConversation conversation = AiConversation.builder()
                .userId(userId)
                .build();

        conversation.addMessage(AiMessage.builder()
                .role(AiMessageRole.USER)
                .content("Explain polymorphism.")
                .build());
        conversation.addMessage(AiMessage.builder()
                .role(AiMessageRole.ASSISTANT)
                .content("Polymorphism lets one interface support multiple implementations.")
                .build());

        AiConversation savedConversation = conversationRepository.saveAndFlush(conversation);

        assertThat(savedConversation.getMessages()).hasSize(2);
        assertThat(messageRepository.findByConversation_Id(
                savedConversation.getId(),
                PageRequest.of(0, 10, Sort.by("createdAt").ascending())
        ).getContent())
                .extracting(AiMessage::getRole)
                .containsExactly(AiMessageRole.USER, AiMessageRole.ASSISTANT);
    }

    @Test
    void messageCanBePersistedForExistingConversation() {
        AiConversation conversation = conversationRepository.saveAndFlush(AiConversation.builder()
                .userId(UUID.randomUUID())
                .build());

        AiMessage savedMessage = messageRepository.save(AiMessage.builder()
                .conversation(conversation)
                .role(AiMessageRole.USER)
                .content("Summarize this lesson.")
                .build());

        assertThat(savedMessage.getId()).isNotNull();
        assertThat(savedMessage.getConversation().getId()).isEqualTo(conversation.getId());
        assertThat(savedMessage.getRole()).isEqualTo(AiMessageRole.USER);
        assertThat(savedMessage.getContent()).isEqualTo("Summarize this lesson.");
    }
}
