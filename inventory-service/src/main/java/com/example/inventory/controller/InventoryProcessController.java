package com.example.inventory.controller;

import com.example.inventory.dto.ReserveInventoryRequest;
import com.example.inventory.entity.InventoryReservation;
import com.example.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory/process")
@CrossOrigin(origins = "*")
public class InventoryProcessController {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryProcessController.class);
    
    @Autowired
    private InventoryService inventoryService;

    @PostMapping("/reserve-and-confirm")
    public ResponseEntity<InventoryReservation> reserveAndConfirmInventory(@Valid @RequestBody ReserveInventoryRequest request) {
        logger.info("Received reserve and confirm inventory request for order: {}", request.getOrderId());
        
        try {
            // Reserve inventory
            InventoryReservation reservation = inventoryService.reserveInventory(request);
            
            // Immediately confirm the reservation
            inventoryService.confirmReservation(reservation.getReservationId());
            
            logger.info("Inventory reserved and confirmed successfully for order: {}, reservation: {}", 
                request.getOrderId(), reservation.getReservationId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
            
        } catch (Exception e) {
            logger.error("Failed to reserve and confirm inventory for order: {}, error: {}", 
                request.getOrderId(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/quick-reserve")
    public ResponseEntity<InventoryReservation> quickReserveInventory(@Valid @RequestBody QuickReserveRequest request) {
        logger.info("Received quick reserve request for product: {}", request.getProductId());
        
        try {
            ReserveInventoryRequest reserveRequest = new ReserveInventoryRequest();
            reserveRequest.setOrderId("QUICK-" + System.currentTimeMillis());
            reserveRequest.setCustomerId(request.getCustomerId());
            
            ReserveInventoryRequest.InventoryItemRequest item = new ReserveInventoryRequest.InventoryItemRequest();
            item.setProductId(request.getProductId());
            item.setQuantity(request.getQuantity());
            reserveRequest.setItems(java.util.List.of(item));
            
            InventoryReservation reservation = inventoryService.reserveInventory(reserveRequest);
            
            logger.info("Quick inventory reservation created: {}", reservation.getReservationId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
            
        } catch (Exception e) {
            logger.error("Failed to quick reserve inventory for product: {}, error: {}", 
                request.getProductId(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/bulk-confirm")
    public ResponseEntity<BulkConfirmResponse> bulkConfirmReservations(@Valid @RequestBody BulkConfirmRequest request) {
        logger.info("Received bulk confirm request for {} reservations", request.getReservationIds().size());
        
        BulkConfirmResponse response = new BulkConfirmResponse();
        
        for (String reservationId : request.getReservationIds()) {
            try {
                inventoryService.confirmReservation(reservationId);
                response.getSuccessfulConfirmations().add(reservationId);
                logger.debug("Confirmed reservation: {}", reservationId);
                
            } catch (Exception e) {
                logger.warn("Failed to confirm reservation: {}, error: {}", reservationId, e.getMessage());
                response.getFailedConfirmations().put(reservationId, e.getMessage());
            }
        }
        
        logger.info("Bulk confirm completed: {} successful, {} failed", 
            response.getSuccessfulConfirmations().size(), 
            response.getFailedConfirmations().size());
        
        return ResponseEntity.ok(response);
    }

    // DTOs
    public static class QuickReserveRequest {
        @jakarta.validation.constraints.NotBlank(message = "Customer ID is required")
        private String customerId;
        
        @jakarta.validation.constraints.NotBlank(message = "Product ID is required")
        private String productId;
        
        @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static class BulkConfirmRequest {
        @jakarta.validation.constraints.NotEmpty(message = "Reservation IDs are required")
        private java.util.List<String> reservationIds;

        public java.util.List<String> getReservationIds() { return reservationIds; }
        public void setReservationIds(java.util.List<String> reservationIds) { this.reservationIds = reservationIds; }
    }

    public static class BulkConfirmResponse {
        private java.util.List<String> successfulConfirmations = new java.util.ArrayList<>();
        private java.util.Map<String, String> failedConfirmations = new java.util.HashMap<>();

        public java.util.List<String> getSuccessfulConfirmations() { return successfulConfirmations; }
        public void setSuccessfulConfirmations(java.util.List<String> successfulConfirmations) { this.successfulConfirmations = successfulConfirmations; }
        public java.util.Map<String, String> getFailedConfirmations() { return failedConfirmations; }
        public void setFailedConfirmations(java.util.Map<String, String> failedConfirmations) { this.failedConfirmations = failedConfirmations; }
    }
}