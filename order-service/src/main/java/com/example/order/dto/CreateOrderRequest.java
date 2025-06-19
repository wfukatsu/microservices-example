package com.example.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public class CreateOrderRequest {
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotEmpty(message = "Order items are required")
    @Valid
    private List<OrderItemRequest> items;
    
    @NotNull(message = "Payment method details are required")
    @Valid
    private PaymentMethodDetails paymentMethodDetails;
    
    @NotNull(message = "Shipping information is required")
    @Valid
    private ShippingInfo shippingInfo;
    
    private String notes;

    // Getters and setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    public PaymentMethodDetails getPaymentMethodDetails() {
        return paymentMethodDetails;
    }

    public void setPaymentMethodDetails(PaymentMethodDetails paymentMethodDetails) {
        this.paymentMethodDetails = paymentMethodDetails;
    }

    public ShippingInfo getShippingInfo() {
        return shippingInfo;
    }

    public void setShippingInfo(ShippingInfo shippingInfo) {
        this.shippingInfo = shippingInfo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public static class OrderItemRequest {
        @NotBlank(message = "Product ID is required")
        private String productId;
        
        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    public static class PaymentMethodDetails {
        @NotBlank(message = "Payment method is required")
        private String paymentMethod;
        
        private String cardNumber;
        private String expiryMonth;
        private String expiryYear;
        private String cvv;
        private String cardholderName;

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public void setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
        }

        public String getExpiryMonth() {
            return expiryMonth;
        }

        public void setExpiryMonth(String expiryMonth) {
            this.expiryMonth = expiryMonth;
        }

        public String getExpiryYear() {
            return expiryYear;
        }

        public void setExpiryYear(String expiryYear) {
            this.expiryYear = expiryYear;
        }

        public String getCvv() {
            return cvv;
        }

        public void setCvv(String cvv) {
            this.cvv = cvv;
        }

        public String getCardholderName() {
            return cardholderName;
        }

        public void setCardholderName(String cardholderName) {
            this.cardholderName = cardholderName;
        }
    }

    public static class ShippingInfo {
        @NotBlank(message = "Shipping method is required")
        private String shippingMethod;
        
        @NotBlank(message = "Carrier is required")
        private String carrier;
        
        @NotNull(message = "Recipient information is required")
        @Valid
        private RecipientInfo recipientInfo;
        
        private PackageInfo packageInfo;

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

        public static class RecipientInfo {
            @NotBlank(message = "Recipient name is required")
            private String name;
            
            @NotBlank(message = "Phone number is required")
            private String phone;
            
            @NotBlank(message = "Address is required")
            private String address;
            
            @NotBlank(message = "City is required")
            private String city;
            
            @NotBlank(message = "State is required")
            private String state;
            
            @NotBlank(message = "Postal code is required")
            private String postalCode;
            
            @NotBlank(message = "Country is required")
            private String country;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getPhone() {
                return phone;
            }

            public void setPhone(String phone) {
                this.phone = phone;
            }

            public String getAddress() {
                return address;
            }

            public void setAddress(String address) {
                this.address = address;
            }

            public String getCity() {
                return city;
            }

            public void setCity(String city) {
                this.city = city;
            }

            public String getState() {
                return state;
            }

            public void setState(String state) {
                this.state = state;
            }

            public String getPostalCode() {
                return postalCode;
            }

            public void setPostalCode(String postalCode) {
                this.postalCode = postalCode;
            }

            public String getCountry() {
                return country;
            }

            public void setCountry(String country) {
                this.country = country;
            }
        }

        public static class PackageInfo {
            private double weight;
            private String dimensions;
            private String specialInstructions;

            public double getWeight() {
                return weight;
            }

            public void setWeight(double weight) {
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
        }
    }
}