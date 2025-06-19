package com.example.shipping.service;

import java.time.LocalDateTime;
import java.util.List;

public interface CarrierIntegrationService {
    CarrierShipmentResponse createShipment(CarrierShipmentRequest request);
    void cancelShipment(String carrier, String trackingNumber);
    TrackingInfo getTrackingInfo(String carrier, String trackingNumber);
}

// CarrierShipmentRequest class
class CarrierShipmentRequest {
    private String shipmentId;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingPostalCode;
    private String shippingCountry;
    private Double weight;
    private String dimensions;
    private String specialInstructions;
    private String shippingMethod;
    private List<com.example.shipping.dto.CreateShipmentRequest.ShippingItemRequest> items;
    
    private CarrierShipmentRequest(Builder builder) {
        this.shipmentId = builder.shipmentId;
        this.recipientName = builder.recipientName;
        this.recipientPhone = builder.recipientPhone;
        this.shippingAddress = builder.shippingAddress;
        this.shippingCity = builder.shippingCity;
        this.shippingState = builder.shippingState;
        this.shippingPostalCode = builder.shippingPostalCode;
        this.shippingCountry = builder.shippingCountry;
        this.weight = builder.weight;
        this.dimensions = builder.dimensions;
        this.specialInstructions = builder.specialInstructions;
        this.shippingMethod = builder.shippingMethod;
        this.items = builder.items;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String shipmentId;
        private String recipientName;
        private String recipientPhone;
        private String shippingAddress;
        private String shippingCity;
        private String shippingState;
        private String shippingPostalCode;
        private String shippingCountry;
        private Double weight;
        private String dimensions;
        private String specialInstructions;
        private String shippingMethod;
        private List<com.example.shipping.dto.CreateShipmentRequest.ShippingItemRequest> items;
        
        public Builder shipmentId(String shipmentId) {
            this.shipmentId = shipmentId;
            return this;
        }
        
        public Builder recipientName(String recipientName) {
            this.recipientName = recipientName;
            return this;
        }
        
        public Builder recipientPhone(String recipientPhone) {
            this.recipientPhone = recipientPhone;
            return this;
        }
        
        public Builder shippingAddress(String shippingAddress) {
            this.shippingAddress = shippingAddress;
            return this;
        }
        
        public Builder shippingCity(String shippingCity) {
            this.shippingCity = shippingCity;
            return this;
        }
        
        public Builder shippingState(String shippingState) {
            this.shippingState = shippingState;
            return this;
        }
        
        public Builder shippingPostalCode(String shippingPostalCode) {
            this.shippingPostalCode = shippingPostalCode;
            return this;
        }
        
        public Builder shippingCountry(String shippingCountry) {
            this.shippingCountry = shippingCountry;
            return this;
        }
        
        public Builder weight(Double weight) {
            this.weight = weight;
            return this;
        }
        
        public Builder dimensions(String dimensions) {
            this.dimensions = dimensions;
            return this;
        }
        
        public Builder specialInstructions(String specialInstructions) {
            this.specialInstructions = specialInstructions;
            return this;
        }
        
        public Builder shippingMethod(String shippingMethod) {
            this.shippingMethod = shippingMethod;
            return this;
        }
        
        public Builder items(List<com.example.shipping.dto.CreateShipmentRequest.ShippingItemRequest> items) {
            this.items = items;
            return this;
        }
        
        public CarrierShipmentRequest build() {
            return new CarrierShipmentRequest(this);
        }
    }
    
    // Getters
    public String getShipmentId() { return shipmentId; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getShippingAddress() { return shippingAddress; }
    public String getShippingCity() { return shippingCity; }
    public String getShippingState() { return shippingState; }
    public String getShippingPostalCode() { return shippingPostalCode; }
    public String getShippingCountry() { return shippingCountry; }
    public Double getWeight() { return weight; }
    public String getDimensions() { return dimensions; }
    public String getSpecialInstructions() { return specialInstructions; }
    public String getShippingMethod() { return shippingMethod; }
    public List<com.example.shipping.dto.CreateShipmentRequest.ShippingItemRequest> getItems() { return items; }
}

// CarrierShipmentResponse class
class CarrierShipmentResponse {
    private String trackingNumber;
    private LocalDateTime estimatedDeliveryDate;
    private String carrierShipmentId;
    
    private CarrierShipmentResponse(Builder builder) {
        this.trackingNumber = builder.trackingNumber;
        this.estimatedDeliveryDate = builder.estimatedDeliveryDate;
        this.carrierShipmentId = builder.carrierShipmentId;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String trackingNumber;
        private LocalDateTime estimatedDeliveryDate;
        private String carrierShipmentId;
        
        public Builder trackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            return this;
        }
        
        public Builder estimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) {
            this.estimatedDeliveryDate = estimatedDeliveryDate;
            return this;
        }
        
        public Builder carrierShipmentId(String carrierShipmentId) {
            this.carrierShipmentId = carrierShipmentId;
            return this;
        }
        
        public CarrierShipmentResponse build() {
            return new CarrierShipmentResponse(this);
        }
    }
    
    // Getters
    public String getTrackingNumber() { return trackingNumber; }
    public LocalDateTime getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public String getCarrierShipmentId() { return carrierShipmentId; }
}

// TrackingInfo class
class TrackingInfo {
    private String trackingNumber;
    private String status;
    private LocalDateTime lastUpdated;
    private String currentLocation;
    private LocalDateTime estimatedDelivery;
    
    private TrackingInfo(Builder builder) {
        this.trackingNumber = builder.trackingNumber;
        this.status = builder.status;
        this.lastUpdated = builder.lastUpdated;
        this.currentLocation = builder.currentLocation;
        this.estimatedDelivery = builder.estimatedDelivery;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String trackingNumber;
        private String status;
        private LocalDateTime lastUpdated;
        private String currentLocation;
        private LocalDateTime estimatedDelivery;
        
        public Builder trackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            return this;
        }
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder lastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }
        
        public Builder currentLocation(String currentLocation) {
            this.currentLocation = currentLocation;
            return this;
        }
        
        public Builder estimatedDelivery(LocalDateTime estimatedDelivery) {
            this.estimatedDelivery = estimatedDelivery;
            return this;
        }
        
        public TrackingInfo build() {
            return new TrackingInfo(this);
        }
    }
    
    // Getters
    public String getTrackingNumber() { return trackingNumber; }
    public String getStatus() { return status; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public String getCurrentLocation() { return currentLocation; }
    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    
    public boolean hasUpdates() {
        return lastUpdated != null && lastUpdated.isAfter(LocalDateTime.now().minusMinutes(10));
    }
}