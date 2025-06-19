package com.example.payment.entity;

import com.scalar.db.io.Column;
import com.scalar.db.io.Key;
import java.time.LocalDateTime;

public class Refund {
    @Column(name = "refund_id", isPrimaryKey = true)
    private String refundId;
    
    @Column(name = "payment_id")
    private String paymentId;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "refund_amount")
    private Long refundAmount;
    
    @Column(name = "currency")
    private String currency;
    
    @Column(name = "refund_reason")
    private String refundReason;
    
    @Column(name = "refund_status")
    private String refundStatus;
    
    @Column(name = "provider_refund_id")
    private String providerRefundId;
    
    @Column(name = "processed_at")
    private Long processedAt;
    
    @Column(name = "created_at")
    private Long createdAt;
    
    @Column(name = "updated_at")
    private Long updatedAt;
    
    // Constructors
    public Refund() {}
    
    public Refund(String refundId, String paymentId, String orderId, Long refundAmount,
                  String currency, String refundReason) {
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.refundAmount = refundAmount;
        this.currency = currency;
        this.refundReason = refundReason;
        this.refundStatus = RefundStatus.PENDING.name();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getRefundId() {
        return refundId;
    }
    
    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }
    
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
    
    public Long getRefundAmount() {
        return refundAmount;
    }
    
    public void setRefundAmount(Long refundAmount) {
        this.refundAmount = refundAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getRefundReason() {
        return refundReason;
    }
    
    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }
    
    public String getRefundStatus() {
        return refundStatus;
    }
    
    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }
    
    public RefundStatus getRefundStatusEnum() {
        return RefundStatus.valueOf(refundStatus);
    }
    
    public void setRefundStatusEnum(RefundStatus status) {
        this.refundStatus = status.name();
    }
    
    public String getProviderRefundId() {
        return providerRefundId;
    }
    
    public void setProviderRefundId(String providerRefundId) {
        this.providerRefundId = providerRefundId;
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
    
    public Key getPartitionKey() {
        return Key.ofText("refund_id", refundId);
    }
    
    @Override
    public String toString() {
        return "Refund{" +
                "refundId='" + refundId + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", refundAmount=" + refundAmount +
                ", currency='" + currency + '\'' +
                ", refundStatus='" + refundStatus + '\'' +
                '}';
    }
}