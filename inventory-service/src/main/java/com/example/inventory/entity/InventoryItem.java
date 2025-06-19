package com.example.inventory.entity;

import com.scalar.db.io.Column;
import com.scalar.db.io.Key;
import java.time.LocalDateTime;

public class InventoryItem {
    @Column(name = "product_id", isPrimaryKey = true)
    private String productId;
    
    @Column(name = "product_name")
    private String productName;
    
    @Column(name = "available_quantity")
    private Integer availableQuantity;
    
    @Column(name = "reserved_quantity")
    private Integer reservedQuantity;
    
    @Column(name = "total_quantity")
    private Integer totalQuantity;
    
    @Column(name = "unit_price")
    private Long unitPrice;
    
    @Column(name = "currency")
    private String currency;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "created_at")
    private Long createdAt;
    
    @Column(name = "updated_at")
    private Long updatedAt;
    
    @Column(name = "version")
    private Integer version;
    
    // Constructors
    public InventoryItem() {}
    
    public InventoryItem(String productId, String productName, Integer totalQuantity, 
                        Long unitPrice, String currency) {
        this.productId = productId;
        this.productName = productName;
        this.availableQuantity = totalQuantity;
        this.reservedQuantity = 0;
        this.totalQuantity = totalQuantity;
        this.unitPrice = unitPrice;
        this.currency = currency;
        this.status = InventoryStatus.ACTIVE.name();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.version = 1;
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
    
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
    
    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
    
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    
    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
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
    
    public InventoryStatus getStatusEnum() {
        return InventoryStatus.valueOf(status);
    }
    
    public void setStatusEnum(InventoryStatus status) {
        this.status = status.name();
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getCreatedAtAsDateTime() {
        return LocalDateTime.ofEpochSecond(createdAt / 1000, (int) (createdAt % 1000) * 1000000);
    }
    
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getUpdatedAtAsDateTime() {
        return LocalDateTime.ofEpochSecond(updatedAt / 1000, (int) (updatedAt % 1000) * 1000000);
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public Key getPartitionKey() {
        return Key.ofText("product_id", productId);
    }
    
    @Override
    public String toString() {
        return "InventoryItem{" +
                "productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", availableQuantity=" + availableQuantity +
                ", reservedQuantity=" + reservedQuantity +
                ", totalQuantity=" + totalQuantity +
                ", unitPrice=" + unitPrice +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                ", version=" + version +
                '}';
    }
}