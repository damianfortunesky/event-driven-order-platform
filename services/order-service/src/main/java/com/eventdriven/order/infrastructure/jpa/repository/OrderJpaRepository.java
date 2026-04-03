package com.eventdriven.order.infrastructure.jpa.repository;

import com.eventdriven.order.infrastructure.jpa.entity.OrderJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {
}
