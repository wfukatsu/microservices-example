package com.example.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.util.List;

public class CreateShipmentRequest {
    @NotBlank(message = "Order ID is required")
    private String orderId;
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotBlank(message = "Shipping method is required")
    private String shippingMethod;
    
    @NotBlank(message = "Carrier is required")
    private String carrier;
    
    @Valid
    @NotNull(message = "Recipient info is required")
    private RecipientInfo recipientInfo;
    
    @Valid
    private PackageInfo packageInfo;
    
    @Valid
    @NotNull(message = "Items are required")
    private List<ShippingItemRequest> items;
    
    // Constructors
    public CreateShipmentRequest() {}
    
    // Getters and Setters
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
    
    public RecipientInfo getRecipientInfo() {
        return recipientInfo;
    }
    
    public void setRecipientInfo(RecipientInfo recipientInfo) {
        this.recipientInfo = recipientInfo;
    }
    
    public PackageInfo getPackageInfo() {
        return packageInfo;
    }
    
    public void setPackageInfo(PackageInfo packageInfo) {
        this.packageInfo = packageInfo;
    }
    
    public List<ShippingItemRequest> getItems() {
        return items;
    }
    
    public void setItems(List<ShippingItemRequest> items) {
        this.items = items;
    }
    
    // Nested classes
    public static class RecipientInfo {
        @NotBlank(message = "Recipient name is required")
        private String name;
        
        private String phone;
        
        @NotBlank(message = "Address is required")
        private String address;
        
        @NotBlank(message = "City is required")
        private String city;
        
        private String state;
        
        @NotBlank(message = "Postal code is required")
        private String postalCode;
        
        @NotBlank(message = "Country is required")
        private String country;
        
        // Getters and Setters
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
        private Double weight;
        private String dimensions;
        private String specialInstructions;
        
        // Getters and Setters
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
        public String getDimensions() { return dimensions; }
        public void setDimensions(String dimensions) { this.dimensions = dimensions; }
        public String getSpecialInstructions() { return specialInstructions; }
        public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }
    }
    
    public static class ShippingItemRequest {
        @NotBlank(message = "Product ID is required")
        private String productId;
        
        @NotBlank(message = "Product name is required")
        private String productName;
        
        @NotNull(message = "Quantity is required")
        private Integer quantity;
        
        private Double weight;
        private String dimensions;
        private Boolean isFragile = false;
        private Boolean isHazardous = false;
        
        // Getters and Setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
        public String getDimensions() { return dimensions; }
        public void setDimensions(String dimensions) { this.dimensions = dimensions; }
        public Boolean getIsFragile() { return isFragile; }
        public void setIsFragile(Boolean isFragile) { this.isFragile = isFragile; }
        public Boolean getIsHazardous() { return isHazardous; }
        public void setIsHazardous(Boolean isHazardous) { this.isHazardous = isHazardous; }
    }
}