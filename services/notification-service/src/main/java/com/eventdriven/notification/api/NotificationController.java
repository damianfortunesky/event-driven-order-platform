package com.eventdriven.notification.api;

import com.eventdriven.notification.api.dto.NotificationResponse;
import com.eventdriven.notification.application.NotificationService;
import com.eventdriven.notification.infrastructure.jpa.entity.NotificationJpaEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public List<NotificationResponse> getNotifications(@RequestParam(required = false) String orderId) {
    List<NotificationJpaEntity> notifications = orderId == null || orderId.isBlank()
        ? notificationService.findAll()
        : notificationService.findByOrderId(orderId);

    return notifications.stream()
        .map(this::toResponse)
        .toList();
  }

  private NotificationResponse toResponse(NotificationJpaEntity entity) {
    return new NotificationResponse(
        entity.getId(),
        entity.getSourceEventId(),
        entity.getSourceEventType(),
        entity.getOrderId(),
        entity.getCustomerId(),
        entity.getRecipientEmail(),
        entity.getChannel(),
        entity.getSubject(),
        entity.getBody(),
        entity.getCorrelationId(),
        entity.getCreatedAt()
    );
  }
}
