package com.example.inventory.controller;

import com.example.inventory.dto.CreateInventoryItemRequest;
import com.example.inventory.dto.ReserveInventoryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestMockMvc
@TestPropertySource(properties = {
    "scalar.db.contact_points=jdbc:sqlite::memory:",
    "logging.level.com.scalar.db=WARN"
})
class InventoryControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void createInventoryItem_Success() throws Exception {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
            "PROD-TEST-001", "Integration Test Product", 50, 2000L, "JPY");
        
        mockMvc.perform(post("/inventory/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpected(jsonPath("$.product_id").value("PROD-TEST-001"))
                .andExpected(jsonPath("$.product_name").value("Integration Test Product"))
                .andExpected(jsonPath("$.total_quantity").value(50))
                .andExpected(jsonPath("$.available_quantity").value(50))
                .andExpected(jsonPath("$.reserved_quantity").value(0));
    }
    
    @Test
    void createInventoryItem_InvalidRequest_ReturnsBadRequest() throws Exception {
        CreateInventoryItemRequest request = new CreateInventoryItemRequest(
            "", "", -1, -100L, "");
        
        mockMvc.perform(post("/inventory/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void getInventoryItem_NotFound() throws Exception {
        mockMvc.perform(get("/inventory/items/NON-EXISTENT"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void reserveInventory_Success() throws Exception {
        // First create an inventory item
        CreateInventoryItemRequest createRequest = new CreateInventoryItemRequest(
            "PROD-TEST-002", "Reserve Test Product", 100, 1500L, "JPY");
        
        mockMvc.perform(post("/inventory/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());
        
        // Then reserve some inventory
        ReserveInventoryRequest reserveRequest = new ReserveInventoryRequest(
            "PROD-TEST-002", "CUST-001", 10);
        reserveRequest.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        mockMvc.perform(post("/inventory/items/PROD-TEST-002/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reserveRequest)))
                .andExpect(status().isCreated())
                .andExpected(jsonPath("$.product_id").value("PROD-TEST-002"))
                .andExpected(jsonPath("$.customer_id").value("CUST-001"))
                .andExpected(jsonPath("$.reserved_quantity").value(10))
                .andExpected(jsonPath("$.reservation_status").value("ACTIVE"));
    }
    
    @Test
    void getAllInventoryItems_Success() throws Exception {
        mockMvc.perform(get("/inventory/items"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$").isArray());
    }
}