package com.eventdriven.inventory.infrastructure.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_stock")
public class InventoryStockJpaEntity {

  @Id
  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(name = "available_quantity", nullable = false)
  private long availableQuantity;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public long getAvailableQuantity() {
    return availableQuantity;
  }

  public void setAvailableQuantity(long availableQuantity) {
    this.availableQuantity = availableQuantity;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
