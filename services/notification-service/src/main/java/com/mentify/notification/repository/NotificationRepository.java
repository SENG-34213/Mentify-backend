package com.mentify.notification.repository;

import com.mentify.notification.entity.EmailDeliveryStatus;
import com.mentify.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByEventIdAndRecipientUserId(UUID eventId, String recipientUserId);

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId, Pageable pageable);

    Page<Notification> findByRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(
            String recipientUserId,
            Pageable pageable
    );

    long countByRecipientUserIdAndReadAtIsNull(String recipientUserId);

    long countByReadAtIsNull();

    long countByEmailStatus(EmailDeliveryStatus emailStatus);

    Optional<Notification> findByIdAndRecipientUserId(UUID id, String recipientUserId);

    int deleteByIdAndRecipientUserId(UUID id, String recipientUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select notification from Notification notification where notification.id = :id")
    Optional<Notification> findByIdForEmailDelivery(@Param("id") UUID id);

    List<Notification> findTop50ByEmailStatusAndNextEmailAttemptAtLessThanEqualOrderByCreatedAtAsc(
            EmailDeliveryStatus emailStatus,
            Instant now
    );

    @Modifying
    @Query("""
            update Notification notification
            set notification.readAt = :readAt
            where notification.recipientUserId = :recipientUserId
              and notification.readAt is null
            """)
    int markAllReadByRecipient(
            @Param("recipientUserId") String recipientUserId,
            @Param("readAt") Instant readAt
    );
}
