package com.example.order.entity;

import com.scalar.db.api.Key;
import com.scalar.db.io.Column;
import com.scalar.db.io.BigIntColumn;
import com.scalar.db.io.IntColumn;
import com.scalar.db.io.TextColumn;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class OrderItem {
    @Column(name = "order_id", isPrimaryKey = true)
    private String orderId;
    
    @Column(name = "product_id", isPrimaryKey = true)
    private String productId;
    
    @Column(name = "product_name")
    private String productName;
    
    @Column(name = "quantity")
    private int quantity;
    
    @Column(name = "unit_price")
    private long unitPrice;
    
    @Column(name = "total_price")
    private long totalPrice;
    
    @Column(name = "currency")
    private String currency;
    
    @Column(name = "weight")
    private long weight;
    
    @Column(name = "created_at")
    private long createdAt;

    public OrderItem() {}

    public OrderItem(String orderId, String productId, String productName, int quantity, BigDecimal unitPrice) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice.movePointRight(2).longValue();
        this.totalPrice = this.unitPrice * quantity;
        this.currency = "JPY";
        this.createdAt = System.currentTimeMillis();
    }

    public Key getPartitionKey() {
        return Key.of(
            TextColumn.of("order_id", orderId),
            TextColumn.of("product_id", productId)
        );
    }

    public Map<String, Column<?>> getColumns() {
        Map<String, Column<?>> columns = new HashMap<>();
        columns.put("order_id", TextColumn.of("order_id", orderId));
        columns.put("product_id", TextColumn.of("product_id", productId));
        columns.put("product_name", TextColumn.of("product_name", productName));
        columns.put("quantity", IntColumn.of("quantity", quantity));
        columns.put("unit_price", BigIntColumn.of("unit_price", unitPrice));
        columns.put("total_price", BigIntColumn.of("total_price", totalPrice));
        columns.put("currency", TextColumn.of("currency", currency));
        columns.put("weight", BigIntColumn.of("weight", weight));
        columns.put("created_at", BigIntColumn.of("created_at", createdAt));
        return columns;
    }

    // Getters and setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.totalPrice = this.unitPrice * quantity;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(long unitPrice) {
        this.unitPrice = unitPrice;
        this.totalPrice = this.unitPrice * quantity;
    }

    public BigDecimal getUnitPriceDecimal() {
        return BigDecimal.valueOf(unitPrice, 2);
    }

    public void setUnitPriceDecimal(BigDecimal unitPrice) {
        this.unitPrice = unitPrice.movePointRight(2).longValue();
        this.totalPrice = this.unitPrice * quantity;
    }

    public long getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(long totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getTotalPriceDecimal() {
        return BigDecimal.valueOf(totalPrice, 2);
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getWeight() {
        return weight;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }

    public BigDecimal getWeightDecimal() {
        return BigDecimal.valueOf(weight, 3);
    }

    public void setWeightDecimal(BigDecimal weight) {
        this.weight = weight.movePointRight(3).longValue();
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
}