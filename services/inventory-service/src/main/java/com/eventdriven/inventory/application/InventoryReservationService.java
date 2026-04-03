package com.eventdriven.inventory.application;

import com.eventdriven.inventory.infrastructure.jpa.entity.InboxEventJpaEntity;
import com.eventdriven.inventory.infrastructure.jpa.entity.InventoryReservationItemJpaEntity;
import com.eventdriven.inventory.infrastructure.jpa.entity.InventoryReservationJpaEntity;
import com.eventdriven.inventory.infrastructure.jpa.entity.InventoryStockJpaEntity;
import com.eventdriven.inventory.infrastructure.jpa.repository.InboxEventJpaRepository;
import com.eventdriven.inventory.infrastructure.jpa.repository.InventoryReservationItemJpaRepository;
import com.eventdriven.inventory.infrastructure.jpa.repository.InventoryReservationJpaRepository;
import com.eventdriven.inventory.infrastructure.jpa.repository.InventoryStockJpaRepository;
import com.eventdriven.inventory.infrastructure.kafka.model.EventEnvelope;
import com.eventdriven.inventory.infrastructure.kafka.model.OrderItemPayload;
import com.eventdriven.inventory.infrastructure.kafka.model.PaymentApprovedPayload;
import com.eventdriven.inventory.infrastructure.kafka.model.ReservedItemPayload;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryReservationService {

  private final InventoryStockJpaRepository stockRepository;
  private final InventoryReservationJpaRepository reservationRepository;
  private final InventoryReservationItemJpaRepository reservationItemRepository;
  private final InboxEventJpaRepository inboxEventRepository;

  public InventoryReservationService(
      InventoryStockJpaRepository stockRepository,
      InventoryReservationJpaRepository reservationRepository,
      InventoryReservationItemJpaRepository reservationItemRepository,
      InboxEventJpaRepository inboxEventRepository
  ) {
    this.stockRepository = stockRepository;
    this.reservationRepository = reservationRepository;
    this.reservationItemRepository = reservationItemRepository;
    this.inboxEventRepository = inboxEventRepository;
  }

  @Transactional
  public ReservationResult reserveFromPaymentApproved(EventEnvelope eventEnvelope, PaymentApprovedPayload payload) {
    if (inboxEventRepository.existsById(eventEnvelope.eventId())) {
      return buildResultFromExisting(eventEnvelope.eventId(), payload.orderId(), eventEnvelope.correlationId());
    }

    return reservationRepository.findByPaymentEventId(eventEnvelope.eventId())
        .map(existing -> {
          markInbox(eventEnvelope);
          return toResult(existing, eventEnvelope.correlationId());
        })
        .orElseGet(() -> processNew(eventEnvelope, payload));
  }

  private ReservationResult processNew(EventEnvelope envelope, PaymentApprovedPayload payload) {
    if (payload.items() == null || payload.items().isEmpty()) {
      throw new IllegalArgumentException("PaymentApproved event does not include order items");
    }

    Map<UUID, Long> requested = aggregateRequestedItems(payload.items());
    List<UUID> productIds = requested.keySet().stream().sorted().toList();
    List<InventoryStockJpaEntity> stocks = stockRepository.findAllByProductIdInForUpdate(productIds);

    if (stocks.size() != productIds.size()) {
      String reason = "ONE_OR_MORE_PRODUCTS_NOT_FOUND";
      return persistFailure(envelope, payload.orderId(), reason, requested);
    }

    for (InventoryStockJpaEntity stock : stocks) {
      long requestedQty = requested.getOrDefault(stock.getProductId(), 0L);
      if (stock.getAvailableQuantity() < requestedQty) {
        String reason = "INSUFFICIENT_STOCK:" + stock.getProductId();
        return persistFailure(envelope, payload.orderId(), reason, requested);
      }
    }

    OffsetDateTime now = OffsetDateTime.now();
    for (InventoryStockJpaEntity stock : stocks) {
      long requestedQty = requested.get(stock.getProductId());
      stock.setAvailableQuantity(stock.getAvailableQuantity() - requestedQty);
      stock.setUpdatedAt(now);
    }
    stockRepository.saveAll(stocks);

    InventoryReservationJpaEntity reservation = new InventoryReservationJpaEntity();
    reservation.setId(UUID.randomUUID());
    reservation.setPaymentEventId(envelope.eventId());
    reservation.setOrderId(payload.orderId());
    reservation.setStatus("RESERVED");
    reservation.setCorrelationId(envelope.correlationId());
    reservation.setCreatedAt(now);
    reservationRepository.save(reservation);

    List<InventoryReservationItemJpaEntity> items = requested.entrySet().stream()
        .map(entry -> {
          InventoryReservationItemJpaEntity item = new InventoryReservationItemJpaEntity();
          item.setReservationId(reservation.getId());
          item.setProductId(entry.getKey());
          item.setQuantity(entry.getValue());
          return item;
        })
        .toList();
    reservationItemRepository.saveAll(items);

    markInbox(envelope);
    return toResult(reservation, envelope.correlationId());
  }

  private ReservationResult persistFailure(
      EventEnvelope envelope,
      UUID orderId,
      String reason,
      Map<UUID, Long> requested
  ) {
    OffsetDateTime now = OffsetDateTime.now();
    InventoryReservationJpaEntity reservation = new InventoryReservationJpaEntity();
    reservation.setId(UUID.randomUUID());
    reservation.setPaymentEventId(envelope.eventId());
    reservation.setOrderId(orderId);
    reservation.setStatus("FAILED");
    reservation.setFailureReason(reason);
    reservation.setCorrelationId(envelope.correlationId());
    reservation.setCreatedAt(now);
    reservationRepository.save(reservation);

    List<InventoryReservationItemJpaEntity> items = requested.entrySet().stream()
        .map(entry -> {
          InventoryReservationItemJpaEntity item = new InventoryReservationItemJpaEntity();
          item.setReservationId(reservation.getId());
          item.setProductId(entry.getKey());
          item.setQuantity(entry.getValue());
          return item;
        })
        .toList();
    reservationItemRepository.saveAll(items);

    markInbox(envelope);
    return toResult(reservation, envelope.correlationId());
  }

  private Map<UUID, Long> aggregateRequestedItems(List<OrderItemPayload> rawItems) {
    Map<UUID, Long> requested = new HashMap<>();
    for (OrderItemPayload item : rawItems) {
      if (item.quantity() <= 0) {
        throw new IllegalArgumentException("Item quantity must be positive for product " + item.productId());
      }
      requested.merge(item.productId(), item.quantity(), Long::sum);
    }
    return requested;
  }

  private void markInbox(EventEnvelope envelope) {
    InboxEventJpaEntity inbox = new InboxEventJpaEntity();
    inbox.setEventId(envelope.eventId());
    inbox.setEventType(envelope.eventType());
    inbox.setCorrelationId(envelope.correlationId());
    inbox.setProcessedAt(OffsetDateTime.now());
    inboxEventRepository.save(inbox);
  }

  private ReservationResult buildResultFromExisting(UUID paymentEventId, UUID orderId, String correlationId) {
    InventoryReservationJpaEntity reservation = reservationRepository.findByPaymentEventId(paymentEventId)
        .orElseThrow(() -> new IllegalStateException("Inbox marks event as processed but reservation was not found"));
    return toResult(reservation, correlationId == null || correlationId.isBlank() ? reservation.getCorrelationId() : correlationId);
  }

  private ReservationResult toResult(InventoryReservationJpaEntity reservation, String correlationId) {
    List<ReservedItemPayload> items = new ArrayList<>();
    for (InventoryReservationItemJpaEntity item : reservationItemRepository.findByReservationId(reservation.getId())) {
      items.add(new ReservedItemPayload(item.getProductId(), item.getQuantity()));
    }

    return new ReservationResult(
        reservation.getId(),
        reservation.getPaymentEventId(),
        reservation.getOrderId(),
        reservation.getStatus(),
        reservation.getFailureReason(),
        correlationId,
        items
    );
  }
}
