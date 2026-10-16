package com.mentify.communication.dto.response;

import com.mentify.communication.enums.GroupMemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {

    private UUID userId;
    private GroupMemberRole role;
    private LocalDateTime joinedAt;
}
