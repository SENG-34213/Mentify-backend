package com.mentify.communication.repository;

import com.mentify.communication.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    boolean existsByGroup_IdAndUserIdAndIsActiveTrue(UUID groupId, UUID userId);

    Optional<GroupMember> findByGroup_IdAndUserIdAndIsActiveTrue(UUID groupId, UUID userId);

    List<GroupMember> findByUserIdAndIsActiveTrue(UUID userId);

    List<GroupMember> findByGroup_IdAndIsActiveTrue(UUID groupId);
}
