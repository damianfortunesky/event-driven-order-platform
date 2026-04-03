package com.eventdriven.order.application.usecase;

import com.eventdriven.order.api.error.NotFoundException;
import com.eventdriven.order.application.port.in.CreateOrderCommand;
import com.eventdriven.order.application.port.in.ProcessFinalEventCommand;
import com.eventdriven.order.application.port.out.OrderEventPublisherPort;
import com.eventdriven.order.application.port.out.OrderRepositoryPort;
import com.eventdriven.order.application.port.out.ProcessedEventRepositoryPort;
import com.eventdriven.order.domain.model.Order;
import com.eventdriven.order.domain.model.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService implements OrderUseCase {
  private final OrderRepositoryPort orderRepository;
  private final OrderEventPublisherPort orderEventPublisher;
  private final ProcessedEventRepositoryPort processedEventRepository;

  @Override
  @Transactional
  public Order createOrder(CreateOrderCommand command) {
    Order order = Order.create(command.customerId(), command.items(), command.totalAmount(), command.currency());
    Order saved = orderRepository.save(order);
    orderEventPublisher.publishOrderCreated(saved, command.correlationId());
    log.info("order.created id={} customerId={} correlationId={}", saved.id(), saved.customerId(), command.correlationId());
    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public Order getOrder(UUID id) {
    return orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Order not found: " + id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Order> listOrders() {
    return orderRepository.findAll();
  }

  @Override
  @Transactional
  public void processFinalEvent(ProcessFinalEventCommand command) {
    if (processedEventRepository.existsByEventId(command.eventId())) {
      log.info("event.duplicate eventId={} type={}", command.eventId(), command.eventType());
      return;
    }

    Order order = orderRepository.findById(command.orderId())
        .orElseThrow(() -> new NotFoundException("Order not found for event: " + command.orderId()));

    applyTransition(order, command.eventType(), command.outcome());

    if (order.status() == OrderStatus.INVENTORY_RESERVED) {
      order.confirm();
    }
    if (order.status() == OrderStatus.PAYMENT_REJECTED || order.status() == OrderStatus.INVENTORY_FAILED) {
      order.cancel();
    }

    orderRepository.save(order);
    processedEventRepository.save(command.eventId(), command.eventType(), Instant.now());

    log.info("order.status.updated id={} status={} eventId={} correlationId={}",
        order.id(), order.status(), command.eventId(), command.correlationId());
  }

  private void applyTransition(Order order, String eventType, String outcome) {
    if ("PaymentProcessed".equalsIgnoreCase(eventType)) {
      if ("APPROVED".equalsIgnoreCase(outcome)) {
        order.markPaymentApproved();
      } else {
        order.markPaymentRejected();
      }
      return;
    }

    if ("InventoryProcessed".equalsIgnoreCase(eventType)) {
      if ("RESERVED".equalsIgnoreCase(outcome)) {
        order.markInventoryReserved();
      } else {
        order.markInventoryFailed();
      }
    }
  }
}
