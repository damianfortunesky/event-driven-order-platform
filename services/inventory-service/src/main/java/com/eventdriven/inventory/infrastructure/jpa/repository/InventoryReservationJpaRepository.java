package com.eventdriven.inventory.infrastructure.jpa.repository;

import com.eventdriven.inventory.infrastructure.jpa.entity.InventoryReservationJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationJpaRepository extends JpaRepository<InventoryReservationJpaEntity, UUID> {

  Optional<InventoryReservationJpaEntity> findByPaymentEventId(UUID paymentEventId);
}
