package com.eventdriven.inventory.infrastructure.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservation_item")
@IdClass(InventoryReservationItemJpaEntity.InventoryReservationItemId.class)
public class InventoryReservationItemJpaEntity {

  @Id
  @Column(name = "reservation_id", nullable = false)
  private UUID reservationId;

  @Id
  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(name = "quantity", nullable = false)
  private long quantity;

  public UUID getReservationId() { return reservationId; }

  public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }

  public UUID getProductId() { return productId; }

  public void setProductId(UUID productId) { this.productId = productId; }

  public long getQuantity() { return quantity; }

  public void setQuantity(long quantity) { this.quantity = quantity; }

  public static class InventoryReservationItemId implements Serializable {
    private UUID reservationId;
    private UUID productId;

    public InventoryReservationItemId() {
    }

    public InventoryReservationItemId(UUID reservationId, UUID productId) {
      this.reservationId = reservationId;
      this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      InventoryReservationItemId that = (InventoryReservationItemId) o;
      return Objects.equals(reservationId, that.reservationId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(reservationId, productId);
    }
  }
}
