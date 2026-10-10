package com.mentify.communication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mentify.communication.dto.request.SendMessageRequest;
import com.mentify.communication.dto.request.UpdateMessageRequest;
import com.mentify.communication.dto.response.MessageResponse;
import com.mentify.communication.dto.response.PageResponse;
import com.mentify.communication.enums.MessageDeleteScope;
import com.mentify.communication.enums.MessageType;
import com.mentify.communication.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MessageController.class,
        properties = "spring.cloud.config.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MessageService messageService;

    @Test
    void sendMessageReturnsCreatedResponse() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        LocalDateTime sentAt = LocalDateTime.of(2026, 9, 6, 10, 30);

        when(messageService.sendMessage(eq(groupId), any(SendMessageRequest.class)))
                .thenReturn(messageResponse(messageId, groupId, senderId, "Tomorrow's lecture starts at 9 AM.", sentAt));

        mockMvc.perform(post("/api/communication/groups/{groupId}/messages", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendMessageRequest.builder()
                                .content("Tomorrow's lecture starts at 9 AM.")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Message sent successfully"))
                .andExpect(jsonPath("$.data.id").value(messageId.toString()))
                .andExpect(jsonPath("$.data.groupId").value(groupId.toString()))
                .andExpect(jsonPath("$.data.senderId").value(senderId.toString()))
                .andExpect(jsonPath("$.data.content").value("Tomorrow's lecture starts at 9 AM."))
                .andExpect(jsonPath("$.data.type").value("TEXT"));

        verify(messageService).sendMessage(eq(groupId), any(SendMessageRequest.class));
    }

    @Test
    void sendMessageRejectsBlankContent() throws Exception {
        mockMvc.perform(post("/api/communication/groups/{groupId}/messages", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendMessageRequest.builder()
                                .content("   ")
                                .build())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(messageService);
    }

    @Test
    void sendMessageRejectsOversizedContent() throws Exception {
        mockMvc.perform(post("/api/communication/groups/{groupId}/messages", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SendMessageRequest.builder()
                                .content("a".repeat(5001))
                                .build())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(messageService);
    }

    @Test
    void getMessageHistoryReturnsPaginatedResponse() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        LocalDateTime sentAt = LocalDateTime.of(2026, 9, 6, 10, 30);

        when(messageService.getMessageHistory(groupId, 0, 30))
                .thenReturn(PageResponse.<MessageResponse>builder()
                        .content(List.of(messageResponse(messageId, groupId, senderId, "Hello everyone", sentAt)))
                        .page(0)
                        .size(30)
                        .totalElements(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/api/communication/groups/{groupId}/messages?page=0&size=30", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Messages fetched successfully"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(messageId.toString()))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(30))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        verify(messageService).getMessageHistory(groupId, 0, 30);
    }

        @Test
        void updateMessageReturnsUpdatedResponse() throws Exception {
                UUID groupId = UUID.randomUUID();
                UUID messageId = UUID.randomUUID();
                UUID senderId = UUID.randomUUID();
                LocalDateTime sentAt = LocalDateTime.of(2026, 9, 6, 10, 30);
                LocalDateTime editedAt = LocalDateTime.of(2026, 9, 6, 10, 35);

                when(messageService.updateMessage(eq(groupId), eq(messageId), any(UpdateMessageRequest.class)))
                                .thenReturn(MessageResponse.builder()
                                                .id(messageId)
                                                .groupId(groupId)
                                                .senderId(senderId)
                                                .content("Updated text")
                                                .type(MessageType.TEXT)
                                                .sentAt(sentAt)
                                                .editedAt(editedAt)
                                                .edited(true)
                                                .deletedForEveryone(false)
                                                .build());

                mockMvc.perform(patch("/api/communication/groups/{groupId}/messages/{messageId}", groupId, messageId)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(UpdateMessageRequest.builder().content("Updated text").build())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Message updated successfully"))
                                .andExpect(jsonPath("$.data.edited").value(true));

                verify(messageService).updateMessage(eq(groupId), eq(messageId), any(UpdateMessageRequest.class));
        }

        @Test
        void deleteMessageForMeReturnsSuccess() throws Exception {
                UUID groupId = UUID.randomUUID();
                UUID messageId = UUID.randomUUID();

                mockMvc.perform(delete("/api/communication/groups/{groupId}/messages/{messageId}?scope=ME", groupId, messageId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Message deleted for you successfully"));

                verify(messageService).deleteMessage(groupId, messageId, MessageDeleteScope.ME);
        }

        @Test
        void deleteMessageForEveryoneReturnsSuccess() throws Exception {
                UUID groupId = UUID.randomUUID();
                UUID messageId = UUID.randomUUID();

                mockMvc.perform(delete("/api/communication/groups/{groupId}/messages/{messageId}?scope=EVERYONE", groupId, messageId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Message deleted for everyone successfully"));

                verify(messageService).deleteMessage(groupId, messageId, MessageDeleteScope.EVERYONE);
        }

    private MessageResponse messageResponse(
            UUID messageId,
            UUID groupId,
            UUID senderId,
            String content,
            LocalDateTime sentAt
    ) {
        return MessageResponse.builder()
                .id(messageId)
                .groupId(groupId)
                .senderId(senderId)
                .content(content)
                .type(MessageType.TEXT)
                .sentAt(sentAt)
                .build();
    }
}
