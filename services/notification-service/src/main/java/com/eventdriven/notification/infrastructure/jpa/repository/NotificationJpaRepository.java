package com.eventdriven.notification.infrastructure.jpa.repository;

import com.eventdriven.notification.infrastructure.jpa.entity.NotificationJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

  boolean existsBySourceEventIdAndSourceEventType(UUID sourceEventId, String sourceEventType);

  List<NotificationJpaEntity> findAllByOrderByCreatedAtDesc();

  List<NotificationJpaEntity> findAllByOrderIdOrderByCreatedAtDesc(String orderId);
}
