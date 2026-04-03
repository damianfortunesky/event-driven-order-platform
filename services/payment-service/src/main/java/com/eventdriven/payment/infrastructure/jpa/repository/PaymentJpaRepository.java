package com.eventdriven.payment.infrastructure.jpa.repository;

import com.eventdriven.payment.infrastructure.jpa.entity.PaymentJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

  Optional<PaymentJpaEntity> findByEventId(UUID eventId);
}
