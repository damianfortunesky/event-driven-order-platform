package com.eventdriven.inventory.infrastructure.jpa.repository;

import com.eventdriven.inventory.infrastructure.jpa.entity.InventoryStockJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InventoryStockJpaRepository extends JpaRepository<InventoryStockJpaEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from InventoryStockJpaEntity s where s.productId in :productIds order by s.productId")
  List<InventoryStockJpaEntity> findAllByProductIdInForUpdate(Collection<UUID> productIds);
}
