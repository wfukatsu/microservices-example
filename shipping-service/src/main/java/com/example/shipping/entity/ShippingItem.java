package com.example.shipping.entity;

import com.scalar.db.io.Column;
import com.scalar.db.io.Key;

public class ShippingItem {
    @Column(name = "shipment_id", isPrimaryKey = true)
    private String shipmentId;
    
    @Column(name = "item_id", isPrimaryKey = true)
    private String itemId;
    
    @Column(name = "product_id")
    private String productId;
    
    @Column(name = "product_name")
    private String productName;
    
    @Column(name = "quantity")
    private Integer quantity;
    
    @Column(name = "weight")
    private Double weight;
    
    @Column(name = "dimensions")
    private String dimensions;
    
    @Column(name = "is_fragile")
    private Boolean isFragile;
    
    @Column(name = "is_hazardous")
    private Boolean isHazardous;
    
    // Constructors
    public ShippingItem() {}
    
    public ShippingItem(String shipmentId, String itemId, String productId, 
                       String productName, Integer quantity) {
        this.shipmentId = shipmentId;
        this.itemId = itemId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.isFragile = false;
        this.isHazardous = false;
    }
    
    // Getters and Setters
    public String getShipmentId() {
        return shipmentId;
    }
    
    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
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
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Double getWeight() {
        return weight;
    }
    
    public void setWeight(Double weight) {
        this.weight = weight;
    }
    
    public String getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
    
    public Boolean getIsFragile() {
        return isFragile;
    }
    
    public void setIsFragile(Boolean isFragile) {
        this.isFragile = isFragile;
    }
    
    public Boolean getIsHazardous() {
        return isHazardous;
    }
    
    public void setIsHazardous(Boolean isHazardous) {
        this.isHazardous = isHazardous;
    }
    
    public Key getPartitionKey() {
        return Key.of("shipment_id", shipmentId, "item_id", itemId);
    }
    
    @Override
    public String toString() {
        return "ShippingItem{" +
                "shipmentId='" + shipmentId + '\'' +
                ", itemId='" + itemId + '\'' +
                ", productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}