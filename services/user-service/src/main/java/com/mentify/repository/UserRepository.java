package com.mentify.repository;

import com.mentify.entity.User;
import com.mentify.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    Optional<User> findByKeycloakUserId(String keycloakUserId);
    boolean existsByKeycloakUserIdAndRole(String keycloakUserId, Role role);
    boolean existsByIdAndRole(UUID id, Role role);
}
