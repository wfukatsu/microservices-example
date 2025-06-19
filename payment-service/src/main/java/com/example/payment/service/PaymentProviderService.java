package com.example.payment.service;

public interface PaymentProviderService {
    PaymentProviderResponse executePayment(PaymentProviderRequest request);
    void cancelPayment(String transactionId);
    RefundProviderResponse processRefund(RefundProviderRequest request);
}

// PaymentProviderRequest class
class PaymentProviderRequest {
    private String paymentId;
    private Long amount;
    private String currency;
    private String paymentMethodId;
    private String orderId;
    private String customerId;
    private java.util.Map<String, Object> providerData;
    private boolean autoCapture;
    
    private PaymentProviderRequest(Builder builder) {
        this.paymentId = builder.paymentId;
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.paymentMethodId = builder.paymentMethodId;
        this.orderId = builder.orderId;
        this.customerId = builder.customerId;
        this.providerData = builder.providerData;
        this.autoCapture = builder.autoCapture;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String paymentId;
        private Long amount;
        private String currency;
        private String paymentMethodId;
        private String orderId;
        private String customerId;
        private java.util.Map<String, Object> providerData;
        private boolean autoCapture;
        
        public Builder paymentId(String paymentId) {
            this.paymentId = paymentId;
            return this;
        }
        
        public Builder amount(Long amount) {
            this.amount = amount;
            return this;
        }
        
        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }
        
        public Builder paymentMethodId(String paymentMethodId) {
            this.paymentMethodId = paymentMethodId;
            return this;
        }
        
        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }
        
        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder providerData(java.util.Map<String, Object> providerData) {
            this.providerData = providerData;
            return this;
        }
        
        public Builder autoCapture(boolean autoCapture) {
            this.autoCapture = autoCapture;
            return this;
        }
        
        public PaymentProviderRequest build() {
            return new PaymentProviderRequest(this);
        }
    }
    
    // Getters
    public String getPaymentId() { return paymentId; }
    public Long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getPaymentMethodId() { return paymentMethodId; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public java.util.Map<String, Object> getProviderData() { return providerData; }
    public boolean isAutoCapture() { return autoCapture; }
}

// PaymentProviderResponse class
class PaymentProviderResponse {
    private boolean success;
    private String transactionId;
    private String failureReason;
    
    private PaymentProviderResponse(Builder builder) {
        this.success = builder.success;
        this.transactionId = builder.transactionId;
        this.failureReason = builder.failureReason;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean success;
        private String transactionId;
        private String failureReason;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }
        
        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }
        
        public PaymentProviderResponse build() {
            return new PaymentProviderResponse(this);
        }
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getTransactionId() { return transactionId; }
    public String getFailureReason() { return failureReason; }
}

// RefundProviderRequest class
class RefundProviderRequest {
    private String originalTransactionId;
    private Long refundAmount;
    private String currency;
    private String reason;
    
    private RefundProviderRequest(Builder builder) {
        this.originalTransactionId = builder.originalTransactionId;
        this.refundAmount = builder.refundAmount;
        this.currency = builder.currency;
        this.reason = builder.reason;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String originalTransactionId;
        private Long refundAmount;
        private String currency;
        private String reason;
        
        public Builder originalTransactionId(String originalTransactionId) {
            this.originalTransactionId = originalTransactionId;
            return this;
        }
        
        public Builder refundAmount(Long refundAmount) {
            this.refundAmount = refundAmount;
            return this;
        }
        
        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }
        
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public RefundProviderRequest build() {
            return new RefundProviderRequest(this);
        }
    }
    
    // Getters
    public String getOriginalTransactionId() { return originalTransactionId; }
    public Long getRefundAmount() { return refundAmount; }
    public String getCurrency() { return currency; }
    public String getReason() { return reason; }
}

// RefundProviderResponse class
class RefundProviderResponse {
    private boolean success;
    private String refundId;
    private String failureReason;
    
    private RefundProviderResponse(Builder builder) {
        this.success = builder.success;
        this.refundId = builder.refundId;
        this.failureReason = builder.failureReason;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean success;
        private String refundId;
        private String failureReason;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder refundId(String refundId) {
            this.refundId = refundId;
            return this;
        }
        
        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }
        
        public RefundProviderResponse build() {
            return new RefundProviderResponse(this);
        }
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getRefundId() { return refundId; }
    public String getFailureReason() { return failureReason; }
}