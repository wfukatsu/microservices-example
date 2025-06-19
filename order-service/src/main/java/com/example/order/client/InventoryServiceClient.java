package com.example.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@FeignClient(
    name = "inventory-service",
    url = "${services.inventory.url}",
    fallback = InventoryServiceClientFallback.class
)
public interface InventoryServiceClient {

    @PostMapping("/api/v1/inventory/reserve")
    InventoryReservationResponse reserveInventory(@RequestBody ReserveInventoryRequest request);

    @PostMapping("/api/v1/inventory/confirm/{reservationId}")
    void confirmReservation(@PathVariable String reservationId);

    @PostMapping("/api/v1/inventory/cancel/{reservationId}")
    void cancelReservation(@PathVariable String reservationId);

    @GetMapping("/api/v1/inventory/check")
    InventoryCheckResponse checkInventory(@RequestParam String productId, @RequestParam int quantity);

    // DTOs
    class ReserveInventoryRequest {
        private String orderId;
        private String customerId;
        private List<InventoryItemRequest> items;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public List<InventoryItemRequest> getItems() { return items; }
        public void setItems(List<InventoryItemRequest> items) { this.items = items; }
    }

    class InventoryItemRequest {
        private String productId;
        private int quantity;

        public InventoryItemRequest() {}

        public InventoryItemRequest(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    class InventoryReservationResponse {
        private String reservationId;
        private String orderId;
        private String customerId;
        private String status;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
        private List<ReservedItem> items;

        public String getReservationId() { return reservationId; }
        public void setReservationId(String reservationId) { this.reservationId = reservationId; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public List<ReservedItem> getItems() { return items; }
        public void setItems(List<ReservedItem> items) { this.items = items; }
    }

    class ReservedItem {
        private String productId;
        private String productName;
        private int reservedQuantity;
        private BigDecimal unitPrice;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getReservedQuantity() { return reservedQuantity; }
        public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }

    class InventoryCheckResponse {
        private String productId;
        private int availableQuantity;
        private boolean available;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getAvailableQuantity() { return availableQuantity; }
        public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
    }
}