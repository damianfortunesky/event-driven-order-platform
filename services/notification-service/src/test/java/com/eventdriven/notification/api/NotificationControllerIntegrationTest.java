package com.eventdriven.notification.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventdriven.notification.infrastructure.jpa.entity.NotificationJpaEntity;
import com.eventdriven.notification.infrastructure.jpa.repository.NotificationJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private NotificationJpaRepository notificationRepository;

  @AfterEach
  void tearDown() {
    notificationRepository.deleteAll();
  }

  @Test
  void shouldReturnNotificationsFilteredByOrderId() throws Exception {
    notificationRepository.save(buildNotification("ord-a", "OrderConfirmed"));
    notificationRepository.save(buildNotification("ord-b", "InventoryFailed"));

    mockMvc.perform(get("/api/notifications").queryParam("orderId", "ord-a"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].orderId").value("ord-a"))
        .andExpect(jsonPath("$[0].sourceEventType").value("OrderConfirmed"));
  }

  private NotificationJpaEntity buildNotification(String orderId, String eventType) {
    UUID eventId = UUID.randomUUID();
    NotificationJpaEntity entity = new NotificationJpaEntity();
    entity.setId(eventId);
    entity.setSourceEventId(eventId);
    entity.setSourceEventType(eventType);
    entity.setOrderId(orderId);
    entity.setCustomerId("cus-1");
    entity.setRecipientEmail("customer@example.com");
    entity.setChannel("EMAIL");
    entity.setSubject("subject");
    entity.setBody("body");
    entity.setCorrelationId("corr-1");
    entity.setCreatedAt(Instant.now());
    return entity;
  }
}
