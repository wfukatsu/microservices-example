package com.example.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateInventoryItemRequest {
    @NotBlank(message = "Product ID is required")
    private String productId;
    
    @NotBlank(message = "Product name is required")
    private String productName;
    
    @NotNull(message = "Total quantity is required")
    @Min(value = 0, message = "Total quantity must be non-negative")
    private Integer totalQuantity;
    
    @NotNull(message = "Unit price is required")
    @Min(value = 0, message = "Unit price must be non-negative")
    private Long unitPrice;
    
    @NotBlank(message = "Currency is required")
    private String currency;
    
    private String status = "ACTIVE";
    
    // Constructors
    public CreateInventoryItemRequest() {}
    
    public CreateInventoryItemRequest(String productId, String productName, Integer totalQuantity,
                                    Long unitPrice, String currency) {
        this.productId = productId;
        this.productName = productName;
        this.totalQuantity = totalQuantity;
        this.unitPrice = unitPrice;
        this.currency = currency;
    }
    
    // Getters and Setters
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public Integer getTotalQuantity() {
        return totalQuantity;
    }
    
    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
    
    public Long getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(Long unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}