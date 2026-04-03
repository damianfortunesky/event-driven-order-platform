package com.eventdriven.inventory.infrastructure.jpa.repository;

import com.eventdriven.inventory.infrastructure.jpa.entity.InventoryReservationItemJpaEntity;
import com.eventdriven.inventory.infrastructure.jpa.entity.InventoryReservationItemJpaEntity.InventoryReservationItemId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationItemJpaRepository
    extends JpaRepository<InventoryReservationItemJpaEntity, InventoryReservationItemId> {

  List<InventoryReservationItemJpaEntity> findByReservationId(UUID reservationId);
}
