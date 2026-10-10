package com.mentify.communication.dto.response;

import com.mentify.communication.enums.GroupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationGroupResponse {

    private UUID id;
    private UUID courseId;
    private String name;
    private String description;
    private GroupStatus status;
}
