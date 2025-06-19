package com.example.payment.entity;

import com.scalar.db.io.Column;
import com.scalar.db.io.Key;
import java.time.LocalDateTime;

public class Payment {
    @Column(name = "payment_id", isPrimaryKey = true)
    private String paymentId;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "amount")
    private Long amount;
    
    @Column(name = "currency")
    private String currency;
    
    @Column(name = "payment_method_type")
    private String paymentMethodType;
    
    @Column(name = "payment_method_id")
    private String paymentMethodId;
    
    @Column(name = "payment_status")
    private String paymentStatus;
    
    @Column(name = "payment_provider")
    private String paymentProvider;
    
    @Column(name = "provider_transaction_id")
    private String providerTransactionId;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "processed_at")
    private Long processedAt;
    
    @Column(name = "created_at")
    private Long createdAt;
    
    @Column(name = "updated_at")
    private Long updatedAt;
    
    @Column(name = "version")
    private Integer version;
    
    // Constructors
    public Payment() {}
    
    public Payment(String paymentId, String orderId, String customerId, Long amount, 
                  String currency, String paymentMethodId, String paymentProvider) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethodId = paymentMethodId;
        this.paymentProvider = paymentProvider;
        this.paymentStatus = PaymentStatus.PENDING.name();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.version = 1;
    }
    
    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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
    
    public Long getAmount() {
        return amount;
    }
    
    public void setAmount(Long amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getPaymentMethodType() {
        return paymentMethodType;
    }
    
    public void setPaymentMethodType(String paymentMethodType) {
        this.paymentMethodType = paymentMethodType;
    }
    
    public PaymentMethodType getPaymentMethodTypeEnum() {
        return paymentMethodType != null ? PaymentMethodType.valueOf(paymentMethodType) : null;
    }
    
    public void setPaymentMethodTypeEnum(PaymentMethodType type) {
        this.paymentMethodType = type != null ? type.name() : null;
    }
    
    public String getPaymentMethodId() {
        return paymentMethodId;
    }
    
    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public PaymentStatus getPaymentStatusEnum() {
        return PaymentStatus.valueOf(paymentStatus);
    }
    
    public void setPaymentStatusEnum(PaymentStatus status) {
        this.paymentStatus = status.name();
    }
    
    public String getPaymentProvider() {
        return paymentProvider;
    }
    
    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }
    
    public String getProviderTransactionId() {
        return providerTransactionId;
    }
    
    public void setProviderTransactionId(String providerTransactionId) {
        this.providerTransactionId = providerTransactionId;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public Long getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(Long processedAt) {
        this.processedAt = processedAt;
    }
    
    public LocalDateTime getProcessedAtAsDateTime() {
        return processedAt != null ? 
            LocalDateTime.ofEpochSecond(processedAt / 1000, (int) (processedAt % 1000) * 1000000) : null;
    }
    
    public void setProcessedAtAsDateTime(LocalDateTime processedAt) {
        this.processedAt = processedAt != null ? 
            processedAt.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() : null;
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
        return Key.ofText("payment_id", paymentId);
    }
    
    @Override
    public String toString() {
        return "Payment{" +
                "paymentId='" + paymentId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", paymentProvider='" + paymentProvider + '\'' +
                ", version=" + version +
                '}';
    }
}