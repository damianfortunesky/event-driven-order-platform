package com.eventdriven.order.infrastructure.jpa.repository;

import com.eventdriven.order.infrastructure.jpa.entity.ProcessedEventJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventJpaEntity, UUID> {
}
