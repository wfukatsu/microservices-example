package com.example.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class UpdateShippingStatusRequest {
    @NotBlank(message = "Shipping status is required")
    private String shippingStatus;
    
    private String trackingNumber;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private String eventLocation;
    private String statusDescription;
    
    // Constructors
    public UpdateShippingStatusRequest() {}
    
    public UpdateShippingStatusRequest(String shippingStatus) {
        this.shippingStatus = shippingStatus;
    }
    
    // Getters and Setters
    public String getShippingStatus() {
        return shippingStatus;
    }
    
    public void setShippingStatus(String shippingStatus) {
        this.shippingStatus = shippingStatus;
    }
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    
    public LocalDateTime getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }
    
    public void setEstimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }
    
    public LocalDateTime getActualDeliveryDate() {
        return actualDeliveryDate;
    }
    
    public void setActualDeliveryDate(LocalDateTime actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }
    
    public String getEventLocation() {
        return eventLocation;
    }
    
    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }
    
    public String getStatusDescription() {
        return statusDescription;
    }
    
    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }
}