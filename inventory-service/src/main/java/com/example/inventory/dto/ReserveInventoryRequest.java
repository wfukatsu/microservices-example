package com.example.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class ReserveInventoryRequest {
    @NotBlank(message = "Product ID is required")
    private String productId;
    
    @NotBlank(message = "Customer ID is required") 
    private String customerId;
    
    @NotNull(message = "Reserved quantity is required")
    @Min(value = 1, message = "Reserved quantity must be positive")
    private Integer reservedQuantity;
    
    private LocalDateTime expiresAt;
    
    // Constructors
    public ReserveInventoryRequest() {}
    
    public ReserveInventoryRequest(String productId, String customerId, Integer reservedQuantity) {
        this.productId = productId;
        this.customerId = customerId;
        this.reservedQuantity = reservedQuantity;
        this.expiresAt = LocalDateTime.now().plusHours(24); // Default 24 hours
    }
    
    // Getters and Setters
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    
    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}