package com.mentify.repository;

import com.mentify.entity.ProcessedStripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProcessedStripeWebhookEventRepository extends JpaRepository<ProcessedStripeWebhookEvent, String> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_stripe_webhook_events (
                event_id,
                event_type,
                stripe_payment_intent_id,
                processed_at
            )
            VALUES (
                :eventId,
                :eventType,
                :stripePaymentIntentId,
                :processedAt
            )
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("stripePaymentIntentId") String stripePaymentIntentId,
            @Param("processedAt") LocalDateTime processedAt
    );
}
