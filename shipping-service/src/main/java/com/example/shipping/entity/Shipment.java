package com.example.shipping.entity;

import com.scalar.db.io.Column;
import com.scalar.db.io.Key;
import java.time.LocalDateTime;

public class Shipment {
    @Column(name = "shipment_id", isPrimaryKey = true)
    private String shipmentId;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "shipping_method")
    private String shippingMethod;
    
    @Column(name = "carrier")
    private String carrier;
    
    @Column(name = "tracking_number")
    private String trackingNumber;
    
    @Column(name = "shipping_status")
    private String shippingStatus;
    
    @Column(name = "recipient_name")
    private String recipientName;
    
    @Column(name = "recipient_phone")
    private String recipientPhone;
    
    @Column(name = "shipping_address")
    private String shippingAddress;
    
    @Column(name = "shipping_city")
    private String shippingCity;
    
    @Column(name = "shipping_state")
    private String shippingState;
    
    @Column(name = "shipping_postal_code")
    private String shippingPostalCode;
    
    @Column(name = "shipping_country")
    private String shippingCountry;
    
    @Column(name = "estimated_delivery_date")
    private Long estimatedDeliveryDate;
    
    @Column(name = "actual_delivery_date")
    private Long actualDeliveryDate;
    
    @Column(name = "shipping_cost")
    private Long shippingCost;
    
    @Column(name = "currency")
    private String currency;
    
    @Column(name = "weight")
    private Double weight;
    
    @Column(name = "dimensions")
    private String dimensions;
    
    @Column(name = "special_instructions")
    private String specialInstructions;
    
    @Column(name = "created_at")
    private Long createdAt;
    
    @Column(name = "updated_at")
    private Long updatedAt;
    
    @Column(name = "version")
    private Integer version;
    
    // Constructors
    public Shipment() {}
    
    public Shipment(String shipmentId, String orderId, String customerId, String carrier) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.carrier = carrier;
        this.shippingStatus = ShippingStatus.PENDING.name();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.version = 1;
    }
    
    // Getters and Setters
    public String getShipmentId() {
        return shipmentId;
    }
    
    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }
    
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
    
    public String getShippingMethod() {
        return shippingMethod;
    }
    
    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }
    
    public String getCarrier() {
        return carrier;
    }
    
    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    
    public String getShippingStatus() {
        return shippingStatus;
    }
    
    public void setShippingStatus(String shippingStatus) {
        this.shippingStatus = shippingStatus;
    }
    
    public ShippingStatus getShippingStatusEnum() {
        return ShippingStatus.valueOf(shippingStatus);
    }
    
    public void setShippingStatusEnum(ShippingStatus status) {
        this.shippingStatus = status.name();
    }
    
    public String getRecipientName() {
        return recipientName;
    }
    
    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }
    
    public String getRecipientPhone() {
        return recipientPhone;
    }
    
    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public String getShippingCity() {
        return shippingCity;
    }
    
    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }
    
    public String getShippingState() {
        return shippingState;
    }
    
    public void setShippingState(String shippingState) {
        this.shippingState = shippingState;
    }
    
    public String getShippingPostalCode() {
        return shippingPostalCode;
    }
    
    public void setShippingPostalCode(String shippingPostalCode) {
        this.shippingPostalCode = shippingPostalCode;
    }
    
    public String getShippingCountry() {
        return shippingCountry;
    }
    
    public void setShippingCountry(String shippingCountry) {
        this.shippingCountry = shippingCountry;
    }
    
    public Long getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }
    
    public void setEstimatedDeliveryDate(Long estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }
    
    public LocalDateTime getEstimatedDeliveryDateAsDateTime() {
        return estimatedDeliveryDate != null ? 
            LocalDateTime.ofEpochSecond(estimatedDeliveryDate / 1000, (int) (estimatedDeliveryDate % 1000) * 1000000) : null;
    }
    
    public void setEstimatedDeliveryDateAsDateTime(LocalDateTime estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate != null ? 
            estimatedDeliveryDate.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() : null;
    }
    
    public Long getActualDeliveryDate() {
        return actualDeliveryDate;
    }
    
    public void setActualDeliveryDate(Long actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }
    
    public LocalDateTime getActualDeliveryDateAsDateTime() {
        return actualDeliveryDate != null ? 
            LocalDateTime.ofEpochSecond(actualDeliveryDate / 1000, (int) (actualDeliveryDate % 1000) * 1000000) : null;
    }
    
    public void setActualDeliveryDateAsDateTime(LocalDateTime actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate != null ? 
            actualDeliveryDate.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() : null;
    }
    
    public Long getShippingCost() {
        return shippingCost;
    }
    
    public void setShippingCost(Long shippingCost) {
        this.shippingCost = shippingCost;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
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
    
    public String getSpecialInstructions() {
        return specialInstructions;
    }
    
    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
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
        return Key.ofText("shipment_id", shipmentId);
    }
    
    @Override
    public String toString() {
        return "Shipment{" +
                "shipmentId='" + shipmentId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", carrier='" + carrier + '\'' +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", shippingStatus='" + shippingStatus + '\'' +
                ", version=" + version +
                '}';
    }
}