package com.mentify.communication.dto.response;

import com.mentify.communication.enums.AttachmentType;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class CommunicationAttachmentResponse {
    UUID id;
    UUID materialId;
    String originalFilename;
    String contentType;
    Long fileSize;
    String checksum;
    AttachmentType attachmentType;
}
