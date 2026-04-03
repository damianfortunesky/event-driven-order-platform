package com.eventdriven.order.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private KafkaTemplate<String, String> kafkaTemplate;

  @Test
  void shouldCreateOrderAndGetById() throws Exception {
    String payload = objectMapper.writeValueAsString(Map.of(
        "customerId", UUID.randomUUID(),
        "items", new Object[]{Map.of("productId", UUID.randomUUID(), "quantity", 2)},
        "totalAmount", 250.50,
        "currency", "USD"
    ));

    String response = mockMvc.perform(post("/api/orders")
            .contentType("application/json")
            .header("X-Correlation-Id", "itest-corr")
            .content(payload))
        .andExpect(status().isCreated())
        .andExpect(header().string("X-Correlation-Id", "itest-corr"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn().getResponse().getContentAsString();

    String id = objectMapper.readTree(response).path("id").asText();

    mockMvc.perform(get("/api/orders/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));
  }
}
