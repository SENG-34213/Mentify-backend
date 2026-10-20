package com.mentify.communication.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @Size(max = 5000, message = "Message content must not exceed 5000 characters")
    private String content;

    @Size(max = 10, message = "A message may contain at most 10 attachments")
    private List<UUID> attachmentIds;

    @AssertTrue(message = "Message content or at least one attachment is required")
    public boolean hasContentOrAttachment() {
        return (content != null && !content.isBlank())
                || (attachmentIds != null && !attachmentIds.isEmpty());
    }
}
