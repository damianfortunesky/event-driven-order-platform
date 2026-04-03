package com.eventdriven.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventdriven.order.application.port.in.CreateOrderCommand;
import com.eventdriven.order.application.port.in.ProcessFinalEventCommand;
import com.eventdriven.order.application.port.out.OrderEventPublisherPort;
import com.eventdriven.order.application.port.out.OrderRepositoryPort;
import com.eventdriven.order.application.port.out.ProcessedEventRepositoryPort;
import com.eventdriven.order.application.usecase.OrderService;
import com.eventdriven.order.domain.model.Order;
import com.eventdriven.order.domain.model.OrderItem;
import com.eventdriven.order.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock
  private OrderRepositoryPort orderRepository;
  @Mock
  private OrderEventPublisherPort orderEventPublisher;
  @Mock
  private ProcessedEventRepositoryPort processedEventRepository;

  @InjectMocks
  private OrderService orderService;

  @Test
  void shouldCreateOrderAndPublishEvent() {
    CreateOrderCommand command = new CreateOrderCommand(
        UUID.randomUUID(),
        List.of(new OrderItem(UUID.randomUUID(), 2)),
        BigDecimal.valueOf(150),
        "USD",
        "corr-1");

    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    Order order = orderService.createOrder(command);

    assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
    verify(orderRepository, times(1)).save(any(Order.class));
    verify(orderEventPublisher, times(1)).publishOrderCreated(any(Order.class), any(String.class));
  }

  @Test
  void shouldIgnoreDuplicateEvent() {
    ProcessFinalEventCommand command = new ProcessFinalEventCommand(
        UUID.randomUUID(), UUID.randomUUID(), "PaymentProcessed", "APPROVED", "corr-2");

    when(processedEventRepository.existsByEventId(command.eventId())).thenReturn(true);

    orderService.processFinalEvent(command);

    verify(orderRepository, never()).findById(any());
    verify(processedEventRepository, never()).save(any(), any(), any(Instant.class));
  }

  @Test
  void shouldUpdateStatusToCancelledWhenPaymentRejected() {
    UUID orderId = UUID.randomUUID();
    Order order = new Order(orderId, UUID.randomUUID(), List.of(new OrderItem(UUID.randomUUID(), 1)),
        BigDecimal.TEN, "USD", OrderStatus.PENDING, Instant.now(), Instant.now());

    ProcessFinalEventCommand command = new ProcessFinalEventCommand(
        UUID.randomUUID(), orderId, "PaymentProcessed", "REJECTED", "corr-3");

    when(processedEventRepository.existsByEventId(command.eventId())).thenReturn(false);
    when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

    orderService.processFinalEvent(command);

    assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
    verify(processedEventRepository).save(any(), any(), any());
  }
}
