package com.mentify.repository;

import com.mentify.entity.User;
import com.mentify.enums.AccountStatus;
import com.mentify.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Page<User> findAllByRole(Role role, Pageable pageable);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByAuthUserId(String authUserId);
    Optional<User> findByIdAndRole(UUID id, Role role);
    boolean existsByAuthUserIdAndRole(String authUserId, Role role);
    boolean existsByIdAndRole(UUID id, Role role);
    List<User> findAllByAuthUserIdInAndRole(List<String> authUserIds, Role role);
    List<User> findAllByRoleOrderByCreatedAtDesc(Role role);
    List<User> findAllByIdInAndRole(List<UUID> ids, Role role);
    List<User> findTop5ByRoleOrderByCreatedAtDesc(Role role);
    long countByRole(Role role);
    long countByAccountStatus(AccountStatus accountStatus);
}
