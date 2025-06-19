package com.example.order.entity;

import com.scalar.db.api.Key;
import com.scalar.db.io.Column;
import com.scalar.db.io.BigIntColumn;
import com.scalar.db.io.TextColumn;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Order {
    @Column(name = "order_id", isPrimaryKey = true)
    private String orderId;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "total_amount")
    private long totalAmount;
    
    @Column(name = "currency")
    private String currency;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "shipping_address")
    private String shippingAddress;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "created_at")
    private long createdAt;
    
    @Column(name = "updated_at")
    private long updatedAt;
    
    @Column(name = "inventory_reservation_id")
    private String inventoryReservationId;
    
    @Column(name = "payment_id")
    private String paymentId;
    
    @Column(name = "shipment_id")
    private String shipmentId;

    public Order() {}

    public Order(String orderId, String customerId, OrderStatus status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status.toString();
        this.currency = "JPY";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Key getPartitionKey() {
        return Key.ofText("order_id", orderId);
    }

    public Map<String, Column<?>> getColumns() {
        Map<String, Column<?>> columns = new HashMap<>();
        columns.put("order_id", TextColumn.of("order_id", orderId));
        columns.put("customer_id", TextColumn.of("customer_id", customerId));
        columns.put("status", TextColumn.of("status", status));
        columns.put("total_amount", BigIntColumn.of("total_amount", totalAmount));
        columns.put("currency", TextColumn.of("currency", currency));
        columns.put("payment_method", TextColumn.of("payment_method", paymentMethod));
        columns.put("shipping_address", TextColumn.of("shipping_address", shippingAddress));
        columns.put("notes", TextColumn.of("notes", notes));
        columns.put("created_at", BigIntColumn.of("created_at", createdAt));
        columns.put("updated_at", BigIntColumn.of("updated_at", updatedAt));
        
        if (inventoryReservationId != null) {
            columns.put("inventory_reservation_id", TextColumn.of("inventory_reservation_id", inventoryReservationId));
        }
        if (paymentId != null) {
            columns.put("payment_id", TextColumn.of("payment_id", paymentId));
        }
        if (shipmentId != null) {
            columns.put("shipment_id", TextColumn.of("shipment_id", shipmentId));
        }
        
        return columns;
    }

    // Getters and setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public OrderStatus getStatusEnum() {
        return OrderStatus.fromString(status);
    }

    public void setStatusEnum(OrderStatus status) {
        this.status = status.toString();
        this.updatedAt = System.currentTimeMillis();
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalAmountDecimal() {
        return BigDecimal.valueOf(totalAmount, 2);
    }

    public void setTotalAmountDecimal(BigDecimal amount) {
        this.totalAmount = amount.movePointRight(2).longValue();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCreatedAtDateTime() {
        return LocalDateTime.ofEpochSecond(createdAt / 1000, (int) (createdAt % 1000) * 1000000, 
                                          java.time.ZoneOffset.UTC);
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getUpdatedAtDateTime() {
        return LocalDateTime.ofEpochSecond(updatedAt / 1000, (int) (updatedAt % 1000) * 1000000, 
                                          java.time.ZoneOffset.UTC);
    }

    public String getInventoryReservationId() {
        return inventoryReservationId;
    }

    public void setInventoryReservationId(String inventoryReservationId) {
        this.inventoryReservationId = inventoryReservationId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }
}