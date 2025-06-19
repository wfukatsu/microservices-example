package com.example.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@FeignClient(
    name = "shipping-service",
    url = "${services.shipping.url}",
    fallback = ShippingServiceClientFallback.class
)
public interface ShippingServiceClient {

    @PostMapping("/api/v1/shipping/shipments")
    ShipmentResponse createShipment(@RequestBody CreateShipmentRequest request);

    @PostMapping("/api/v1/shipping/shipments/{shipmentId}/cancel")
    void cancelShipment(@PathVariable String shipmentId);

    @GetMapping("/api/v1/shipping/shipments/{shipmentId}")
    ShipmentResponse getShipment(@PathVariable String shipmentId);

    // DTOs
    class CreateShipmentRequest {
        private String orderId;
        private String customerId;
        private String shippingMethod;
        private String carrier;
        private RecipientInfo recipientInfo;
        private PackageInfo packageInfo;
        private List<ShippingItemRequest> items;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getShippingMethod() { return shippingMethod; }
        public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }
        public String getCarrier() { return carrier; }
        public void setCarrier(String carrier) { this.carrier = carrier; }
        public RecipientInfo getRecipientInfo() { return recipientInfo; }
        public void setRecipientInfo(RecipientInfo recipientInfo) { this.recipientInfo = recipientInfo; }
        public PackageInfo getPackageInfo() { return packageInfo; }
        public void setPackageInfo(PackageInfo packageInfo) { this.packageInfo = packageInfo; }
        public List<ShippingItemRequest> getItems() { return items; }
        public void setItems(List<ShippingItemRequest> items) { this.items = items; }

        public static class RecipientInfo {
            private String name;
            private String phone;
            private String address;
            private String city;
            private String state;
            private String postalCode;
            private String country;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getPhone() { return phone; }
            public void setPhone(String phone) { this.phone = phone; }
            public String getAddress() { return address; }
            public void setAddress(String address) { this.address = address; }
            public String getCity() { return city; }
            public void setCity(String city) { this.city = city; }
            public String getState() { return state; }
            public void setState(String state) { this.state = state; }
            public String getPostalCode() { return postalCode; }
            public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
            public String getCountry() { return country; }
            public void setCountry(String country) { this.country = country; }
        }

        public static class PackageInfo {
            private double weight;
            private String dimensions;
            private String specialInstructions;

            public double getWeight() { return weight; }
            public void setWeight(double weight) { this.weight = weight; }
            public String getDimensions() { return dimensions; }
            public void setDimensions(String dimensions) { this.dimensions = dimensions; }
            public String getSpecialInstructions() { return specialInstructions; }
            public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
        }

        public static class ShippingItemRequest {
            private String productId;
            private String productName;
            private int quantity;
            private double weight;

            public String getProductId() { return productId; }
            public void setProductId(String productId) { this.productId = productId; }
            public String getProductName() { return productName; }
            public void setProductName(String productName) { this.productName = productName; }
            public int getQuantity() { return quantity; }
            public void setQuantity(int quantity) { this.quantity = quantity; }
            public double getWeight() { return weight; }
            public void setWeight(double weight) { this.weight = weight; }
        }
    }

    class ShipmentResponse {
        private String shipmentId;
        private String orderId;
        private String customerId;
        private String status;
        private String carrier;
        private String trackingNumber;
        private String shippingMethod;
        private LocalDateTime estimatedDeliveryDate;
        private LocalDateTime createdAt;

        public String getShipmentId() { return shipmentId; }
        public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCarrier() { return carrier; }
        public void setCarrier(String carrier) { this.carrier = carrier; }
        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
        public String getShippingMethod() { return shippingMethod; }
        public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }
        public LocalDateTime getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
        public void setEstimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) { this.estimatedDeliveryDate = estimatedDeliveryDate; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}