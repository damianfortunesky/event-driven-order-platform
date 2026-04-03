package com.eventdriven.inventory.infrastructure.jpa.repository;

import com.eventdriven.inventory.infrastructure.jpa.entity.InboxEventJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxEventJpaRepository extends JpaRepository<InboxEventJpaEntity, UUID> {
}
