package com.example.shipping.controller;

import com.example.shipping.dto.CreateShipmentRequest;
import com.example.shipping.dto.UpdateShippingStatusRequest;
import com.example.shipping.entity.Shipment;
import com.example.shipping.entity.ShippingItem;
import com.example.shipping.service.ShippingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/shipments")
public class ShippingController {
    
    @Autowired
    private ShippingService shippingService;
    
    @PostMapping
    public ResponseEntity<Shipment> createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        Shipment shipment = shippingService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(shipment);
    }
    
    @GetMapping("/{shipmentId}")
    public ResponseEntity<Shipment> getShipment(@PathVariable String shipmentId) {
        Optional<Shipment> shipment = shippingService.getShipment(shipmentId);
        return shipment.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<Shipment>> getShipments(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String status) {
        
        if (customerId != null) {
            List<Shipment> shipments = shippingService.getShipmentsByCustomer(customerId);
            return ResponseEntity.ok(shipments);
        }
        
        // For simplicity, just return customer shipments if no specific filter
        // In a real implementation, you'd have more sophisticated filtering
        return ResponseEntity.ok(List.of());
    }
    
    @PutMapping("/{shipmentId}/status")
    public ResponseEntity<Shipment> updateShippingStatus(
            @PathVariable String shipmentId,
            @Valid @RequestBody UpdateShippingStatusRequest request) {
        Shipment shipment = shippingService.updateShippingStatus(shipmentId, request);
        return ResponseEntity.ok(shipment);
    }
    
    @PostMapping("/{shipmentId}/cancel")
    public ResponseEntity<Void> cancelShipment(@PathVariable String shipmentId) {
        shippingService.cancelShipment(shipmentId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{shipmentId}/items")
    public ResponseEntity<List<ShippingItem>> getShipmentItems(@PathVariable String shipmentId) {
        List<ShippingItem> items = shippingService.getShipmentItems(shipmentId);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/{shipmentId}/tracking")
    public ResponseEntity<TrackingInfoResponse> getTrackingInfo(@PathVariable String shipmentId) {
        Optional<Shipment> shipmentOpt = shippingService.getShipment(shipmentId);
        
        if (shipmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Shipment shipment = shipmentOpt.get();
        TrackingInfoResponse response = TrackingInfoResponse.builder()
            .shipmentId(shipmentId)
            .trackingNumber(shipment.getTrackingNumber())
            .carrier(shipment.getCarrier())
            .status(shipment.getShippingStatus())
            .estimatedDeliveryDate(shipment.getEstimatedDeliveryDateAsDateTime())
            .actualDeliveryDate(shipment.getActualDeliveryDateAsDateTime())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    // Response DTO for tracking info
    public static class TrackingInfoResponse {
        private String shipmentId;
        private String trackingNumber;
        private String carrier;
        private String status;
        private java.time.LocalDateTime estimatedDeliveryDate;
        private java.time.LocalDateTime actualDeliveryDate;
        
        private TrackingInfoResponse(Builder builder) {
            this.shipmentId = builder.shipmentId;
            this.trackingNumber = builder.trackingNumber;
            this.carrier = builder.carrier;
            this.status = builder.status;
            this.estimatedDeliveryDate = builder.estimatedDeliveryDate;
            this.actualDeliveryDate = builder.actualDeliveryDate;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String shipmentId;
            private String trackingNumber;
            private String carrier;
            private String status;
            private java.time.LocalDateTime estimatedDeliveryDate;
            private java.time.LocalDateTime actualDeliveryDate;
            
            public Builder shipmentId(String shipmentId) {
                this.shipmentId = shipmentId;
                return this;
            }
            
            public Builder trackingNumber(String trackingNumber) {
                this.trackingNumber = trackingNumber;
                return this;
            }
            
            public Builder carrier(String carrier) {
                this.carrier = carrier;
                return this;
            }
            
            public Builder status(String status) {
                this.status = status;
                return this;
            }
            
            public Builder estimatedDeliveryDate(java.time.LocalDateTime estimatedDeliveryDate) {
                this.estimatedDeliveryDate = estimatedDeliveryDate;
                return this;
            }
            
            public Builder actualDeliveryDate(java.time.LocalDateTime actualDeliveryDate) {
                this.actualDeliveryDate = actualDeliveryDate;
                return this;
            }
            
            public TrackingInfoResponse build() {
                return new TrackingInfoResponse(this);
            }
        }
        
        // Getters
        public String getShipmentId() { return shipmentId; }
        public String getTrackingNumber() { return trackingNumber; }
        public String getCarrier() { return carrier; }
        public String getStatus() { return status; }
        public java.time.LocalDateTime getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
        public java.time.LocalDateTime getActualDeliveryDate() { return actualDeliveryDate; }
    }
}