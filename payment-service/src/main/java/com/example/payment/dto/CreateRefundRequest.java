package com.example.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateRefundRequest {
    @NotNull(message = "Refund amount is required")
    @Min(value = 1, message = "Refund amount must be positive")
    private Long refundAmount;
    
    @NotBlank(message = "Currency is required")
    private String currency;
    
    @NotBlank(message = "Refund reason is required")
    private String refundReason;
    
    // Constructors
    public CreateRefundRequest() {}
    
    public CreateRefundRequest(Long refundAmount, String currency, String refundReason) {
        this.refundAmount = refundAmount;
        this.currency = currency;
        this.refundReason = refundReason;
    }
    
    // Getters and Setters
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
}