package com.mentify.repository;

import com.mentify.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuthUser> findByEmailIgnoreCase(String email);
}
