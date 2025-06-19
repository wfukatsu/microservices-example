package com.example.inventory.entity;

import com.scalar.db.io.Column;
import com.scalar.db.io.Key;
import java.time.LocalDateTime;

public class InventoryReservation {
    @Column(name = "reservation_id", isPrimaryKey = true)
    private String reservationId;
    
    @Column(name = "product_id")
    private String productId;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "reserved_quantity")
    private Integer reservedQuantity;
    
    @Column(name = "reservation_status")
    private String reservationStatus;
    
    @Column(name = "expires_at")
    private Long expiresAt;
    
    @Column(name = "created_at")
    private Long createdAt;
    
    @Column(name = "updated_at")
    private Long updatedAt;
    
    // Constructors
    public InventoryReservation() {}
    
    public InventoryReservation(String reservationId, String productId, String customerId,
                               Integer reservedQuantity, LocalDateTime expiresAt) {
        this.reservationId = reservationId;
        this.productId = productId;
        this.customerId = customerId;
        this.reservedQuantity = reservedQuantity;
        this.reservationStatus = ReservationStatus.ACTIVE.name();
        this.expiresAt = expiresAt.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getReservationId() {
        return reservationId;
    }
    
    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }
    
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
    
    public String getReservationStatus() {
        return reservationStatus;
    }
    
    public void setReservationStatus(String reservationStatus) {
        this.reservationStatus = reservationStatus;
    }
    
    public ReservationStatus getReservationStatusEnum() {
        return ReservationStatus.valueOf(reservationStatus);
    }
    
    public void setReservationStatusEnum(ReservationStatus status) {
        this.reservationStatus = status.name();
    }
    
    public Long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public LocalDateTime getExpiresAtAsDateTime() {
        return LocalDateTime.ofEpochSecond(expiresAt / 1000, (int) (expiresAt % 1000) * 1000000);
    }
    
    public void setExpiresAtAsDateTime(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
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
    
    public Key getPartitionKey() {
        return Key.ofText("reservation_id", reservationId);
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
    
    @Override
    public String toString() {
        return "InventoryReservation{" +
                "reservationId='" + reservationId + '\'' +
                ", productId='" + productId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", reservedQuantity=" + reservedQuantity +
                ", reservationStatus='" + reservationStatus + '\'' +
                ", expiresAt=" + expiresAt +
                '}';
    }
}