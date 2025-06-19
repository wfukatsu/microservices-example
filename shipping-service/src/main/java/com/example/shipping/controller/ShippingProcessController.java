package com.example.shipping.controller;

import com.example.shipping.dto.CreateShipmentRequest;
import com.example.shipping.dto.UpdateShippingStatusRequest;
import com.example.shipping.entity.Shipment;
import com.example.shipping.service.ShippingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping/process")
@CrossOrigin(origins = "*")
public class ShippingProcessController {
    
    private static final Logger logger = LoggerFactory.getLogger(ShippingProcessController.class);
    
    @Autowired
    private ShippingService shippingService;

    @PostMapping("/quick-ship")
    public ResponseEntity<Shipment> quickShip(@Valid @RequestBody QuickShipRequest request) {
        logger.info("Received quick ship request for customer: {}", request.getCustomerId());
        
        try {
            CreateShipmentRequest shipmentRequest = new CreateShipmentRequest();
            shipmentRequest.setOrderId("QUICK-SHIP-" + System.currentTimeMillis());
            shipmentRequest.setCustomerId(request.getCustomerId());
            shipmentRequest.setShippingMethod(request.getShippingMethod());
            shipmentRequest.setCarrier(request.getCarrier());
            shipmentRequest.setRecipientInfo(request.getRecipientInfo());
            shipmentRequest.setPackageInfo(request.getPackageInfo());
            
            // Create a single item from the request
            CreateShipmentRequest.ShippingItemRequest item = new CreateShipmentRequest.ShippingItemRequest();
            item.setProductId(request.getProductId());
            item.setProductName(request.getProductName());
            item.setQuantity(request.getQuantity());
            item.setWeight(request.getWeight());
            shipmentRequest.setItems(List.of(item));
            
            Shipment shipment = shippingService.createShipment(shipmentRequest);
            
            logger.info("Quick shipment created successfully: {}", shipment.getShipmentId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(shipment);
            
        } catch (Exception e) {
            logger.error("Failed to create quick shipment for customer: {}, error: {}", 
                request.getCustomerId(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/create-and-ship")
    public ResponseEntity<Shipment> createAndShip(@Valid @RequestBody CreateShipmentRequest request) {
        logger.info("Received create and ship request for order: {}", request.getOrderId());
        
        try {
            // Create shipment
            Shipment shipment = shippingService.createShipment(request);
            
            // Immediately update status to SHIPPED if tracking number is available
            if (shipment.getTrackingNumber() != null && !shipment.getTrackingNumber().isEmpty()) {
                UpdateShippingStatusRequest statusRequest = new UpdateShippingStatusRequest("SHIPPED");
                statusRequest.setTrackingNumber(shipment.getTrackingNumber());
                shipment = shippingService.updateShippingStatus(shipment.getShipmentId(), statusRequest);
            }
            
            logger.info("Shipment created and shipped successfully for order: {}, shipment: {}", 
                request.getOrderId(), shipment.getShipmentId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(shipment);
            
        } catch (Exception e) {
            logger.error("Failed to create and ship for order: {}, error: {}", 
                request.getOrderId(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/bulk-status-update")
    public ResponseEntity<BulkStatusUpdateResponse> bulkStatusUpdate(@Valid @RequestBody BulkStatusUpdateRequest request) {
        logger.info("Received bulk status update request for {} shipments", request.getStatusUpdates().size());
        
        BulkStatusUpdateResponse response = new BulkStatusUpdateResponse();
        
        for (BulkStatusUpdateRequest.StatusUpdateItem updateItem : request.getStatusUpdates()) {
            try {
                UpdateShippingStatusRequest statusRequest = new UpdateShippingStatusRequest(updateItem.getNewStatus());
                statusRequest.setTrackingNumber(updateItem.getTrackingNumber());
                
                Shipment shipment = shippingService.updateShippingStatus(updateItem.getShipmentId(), statusRequest);
                
                BulkStatusUpdateResponse.UpdateResult result = new BulkStatusUpdateResponse.UpdateResult();
                result.setShipmentId(updateItem.getShipmentId());
                result.setOldStatus(updateItem.getCurrentStatus());
                result.setNewStatus(shipment.getShippingStatus());
                result.setTrackingNumber(shipment.getTrackingNumber());
                response.getSuccessfulUpdates().add(result);
                
                logger.debug("Updated shipment status: {}", updateItem.getShipmentId());
                
            } catch (Exception e) {
                logger.warn("Failed to update shipment status: {}, error: {}", updateItem.getShipmentId(), e.getMessage());
                response.getFailedUpdates().put(updateItem.getShipmentId(), e.getMessage());
            }
        }
        
        logger.info("Bulk status update completed: {} successful, {} failed", 
            response.getSuccessfulUpdates().size(), 
            response.getFailedUpdates().size());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/express-delivery")
    public ResponseEntity<Shipment> expressDelivery(@Valid @RequestBody ExpressDeliveryRequest request) {
        logger.info("Received express delivery request for order: {}", request.getOrderId());
        
        try {
            CreateShipmentRequest shipmentRequest = new CreateShipmentRequest();
            shipmentRequest.setOrderId(request.getOrderId());
            shipmentRequest.setCustomerId(request.getCustomerId());
            shipmentRequest.setShippingMethod("EXPRESS");
            shipmentRequest.setCarrier(request.getCarrier() != null ? request.getCarrier() : "YAMATO");
            shipmentRequest.setRecipientInfo(request.getRecipientInfo());
            
            // Set express package info
            CreateShipmentRequest.PackageInfo packageInfo = new CreateShipmentRequest.PackageInfo();
            packageInfo.setWeight(request.getWeight());
            packageInfo.setDimensions(request.getDimensions());
            packageInfo.setSpecialInstructions("EXPRESS DELIVERY - PRIORITY HANDLING");
            shipmentRequest.setPackageInfo(packageInfo);
            
            shipmentRequest.setItems(request.getItems());
            
            Shipment shipment = shippingService.createShipment(shipmentRequest);
            
            logger.info("Express delivery shipment created successfully for order: {}, shipment: {}", 
                request.getOrderId(), shipment.getShipmentId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(shipment);
            
        } catch (Exception e) {
            logger.error("Failed to create express delivery for order: {}, error: {}", 
                request.getOrderId(), e.getMessage());
            throw e;
        }
    }

    // DTOs
    public static class QuickShipRequest {
        @jakarta.validation.constraints.NotBlank(message = "Customer ID is required")
        private String customerId;
        
        @jakarta.validation.constraints.NotBlank(message = "Product ID is required")
        private String productId;
        
        private String productName;
        
        @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;
        
        private double weight;
        
        @jakarta.validation.constraints.NotBlank(message = "Shipping method is required")
        private String shippingMethod;
        
        @jakarta.validation.constraints.NotBlank(message = "Carrier is required")
        private String carrier;
        
        @jakarta.validation.constraints.NotNull(message = "Recipient info is required")
        private CreateShipmentRequest.RecipientInfo recipientInfo;
        
        private CreateShipmentRequest.PackageInfo packageInfo;

        // Getters and setters
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        public String getShippingMethod() { return shippingMethod; }
        public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }
        public String getCarrier() { return carrier; }
        public void setCarrier(String carrier) { this.carrier = carrier; }
        public CreateShipmentRequest.RecipientInfo getRecipientInfo() { return recipientInfo; }
        public void setRecipientInfo(CreateShipmentRequest.RecipientInfo recipientInfo) { this.recipientInfo = recipientInfo; }
        public CreateShipmentRequest.PackageInfo getPackageInfo() { return packageInfo; }
        public void setPackageInfo(CreateShipmentRequest.PackageInfo packageInfo) { this.packageInfo = packageInfo; }
    }

    public static class BulkStatusUpdateRequest {
        @jakarta.validation.constraints.NotEmpty(message = "Status updates are required")
        private java.util.List<StatusUpdateItem> statusUpdates;

        public java.util.List<StatusUpdateItem> getStatusUpdates() { return statusUpdates; }
        public void setStatusUpdates(java.util.List<StatusUpdateItem> statusUpdates) { this.statusUpdates = statusUpdates; }

        public static class StatusUpdateItem {
            @jakarta.validation.constraints.NotBlank(message = "Shipment ID is required")
            private String shipmentId;
            
            private String currentStatus;
            
            @jakarta.validation.constraints.NotBlank(message = "New status is required")
            private String newStatus;
            
            private String trackingNumber;

            public String getShipmentId() { return shipmentId; }
            public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }
            public String getCurrentStatus() { return currentStatus; }
            public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
            public String getNewStatus() { return newStatus; }
            public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
            public String getTrackingNumber() { return trackingNumber; }
            public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        }
    }

    public static class BulkStatusUpdateResponse {
        private java.util.List<UpdateResult> successfulUpdates = new java.util.ArrayList<>();
        private java.util.Map<String, String> failedUpdates = new java.util.HashMap<>();

        public java.util.List<UpdateResult> getSuccessfulUpdates() { return successfulUpdates; }
        public void setSuccessfulUpdates(java.util.List<UpdateResult> successfulUpdates) { this.successfulUpdates = successfulUpdates; }
        public java.util.Map<String, String> getFailedUpdates() { return failedUpdates; }
        public void setFailedUpdates(java.util.Map<String, String> failedUpdates) { this.failedUpdates = failedUpdates; }

        public static class UpdateResult {
            private String shipmentId;
            private String oldStatus;
            private String newStatus;
            private String trackingNumber;

            public String getShipmentId() { return shipmentId; }
            public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }
            public String getOldStatus() { return oldStatus; }
            public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
            public String getNewStatus() { return newStatus; }
            public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
            public String getTrackingNumber() { return trackingNumber; }
            public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        }
    }

    public static class ExpressDeliveryRequest {
        @jakarta.validation.constraints.NotBlank(message = "Order ID is required")
        private String orderId;
        
        @jakarta.validation.constraints.NotBlank(message = "Customer ID is required")
        private String customerId;
        
        private String carrier;
        
        @jakarta.validation.constraints.NotNull(message = "Recipient info is required")
        private CreateShipmentRequest.RecipientInfo recipientInfo;
        
        private double weight;
        private String dimensions;
        
        @jakarta.validation.constraints.NotEmpty(message = "Items are required")
        private java.util.List<CreateShipmentRequest.ShippingItemRequest> items;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getCarrier() { return carrier; }
        public void setCarrier(String carrier) { this.carrier = carrier; }
        public CreateShipmentRequest.RecipientInfo getRecipientInfo() { return recipientInfo; }
        public void setRecipientInfo(CreateShipmentRequest.RecipientInfo recipientInfo) { this.recipientInfo = recipientInfo; }
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        public String getDimensions() { return dimensions; }
        public void setDimensions(String dimensions) { this.dimensions = dimensions; }
        public java.util.List<CreateShipmentRequest.ShippingItemRequest> getItems() { return items; }
        public void setItems(java.util.List<CreateShipmentRequest.ShippingItemRequest> items) { this.items = items; }
    }
}