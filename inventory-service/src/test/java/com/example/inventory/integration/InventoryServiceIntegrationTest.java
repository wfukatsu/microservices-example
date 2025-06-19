package com.example.inventory.integration;

import com.example.inventory.InventoryServiceApplication;
import com.example.inventory.dto.ReserveInventoryRequest;
import com.example.inventory.entity.InventoryReservation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(
    classes = InventoryServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class InventoryServiceIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void reserveInventory_ValidRequest_ReturnsReservation() throws Exception {
        // Given
        ReserveInventoryRequest request = createReserveInventoryRequest();
        
        // When
        ResponseEntity<InventoryReservation> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/inventory/reserve",
            request,
            InventoryReservation.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        
        InventoryReservation reservation = response.getBody();
        assertThat(reservation.getOrderId()).isEqualTo("ORD-001");
        assertThat(reservation.getCustomerId()).isEqualTo("CUST-001");
        assertThat(reservation.getStatus()).isEqualTo("RESERVED");
        assertThat(reservation.getReservationId()).isNotNull();
    }
    
    @Test
    void confirmReservation_ValidReservationId_SuccessfullyConfirms() throws Exception {
        // Given - First create a reservation
        ReserveInventoryRequest reserveRequest = createReserveInventoryRequest();
        ResponseEntity<InventoryReservation> reserveResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/inventory/reserve",
            reserveRequest,
            InventoryReservation.class
        );
        
        String reservationId = reserveResponse.getBody().getReservationId();
        
        // When
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/inventory/confirm/" + reservationId,
            null,
            Void.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify reservation status changed
        ResponseEntity<InventoryReservation> getResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/inventory/reservations/" + reservationId,
            InventoryReservation.class
        );
        
        assertThat(getResponse.getBody().getStatus()).isEqualTo("CONFIRMED");
    }
    
    @Test
    void cancelReservation_ValidReservationId_SuccessfullyCancels() throws Exception {
        // Given - First create a reservation
        ReserveInventoryRequest reserveRequest = createReserveInventoryRequest();
        ResponseEntity<InventoryReservation> reserveResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/inventory/reserve",
            reserveRequest,
            InventoryReservation.class
        );
        
        String reservationId = reserveResponse.getBody().getReservationId();
        
        // When
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/inventory/cancel/" + reservationId,
            null,
            Void.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify reservation status changed
        ResponseEntity<InventoryReservation> getResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/inventory/reservations/" + reservationId,
            InventoryReservation.class
        );
        
        assertThat(getResponse.getBody().getStatus()).isEqualTo("CANCELLED");
    }
    
    @Test
    void checkInventory_ExistingProduct_ReturnsAvailability() throws Exception {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/inventory/check?productId=PROD-001&quantity=1",
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"available\"");
    }
    
    private ReserveInventoryRequest createReserveInventoryRequest() {
        ReserveInventoryRequest request = new ReserveInventoryRequest();
        request.setOrderId("ORD-001");
        request.setCustomerId("CUST-001");
        
        ReserveInventoryRequest.InventoryItemRequest item = new ReserveInventoryRequest.InventoryItemRequest();
        item.setProductId("PROD-001");
        item.setQuantity(1);
        
        request.setItems(List.of(item));
        
        return request;
    }
}