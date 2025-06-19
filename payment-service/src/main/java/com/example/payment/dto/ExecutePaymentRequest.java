package com.example.payment.dto;

import java.util.Map;

public class ExecutePaymentRequest {
    private Map<String, Object> paymentProviderData;
    private boolean autoCapture = true;
    
    // Constructors
    public ExecutePaymentRequest() {}
    
    public ExecutePaymentRequest(Map<String, Object> paymentProviderData, boolean autoCapture) {
        this.paymentProviderData = paymentProviderData;
        this.autoCapture = autoCapture;
    }
    
    // Getters and Setters
    public Map<String, Object> getPaymentProviderData() {
        return paymentProviderData;
    }
    
    public void setPaymentProviderData(Map<String, Object> paymentProviderData) {
        this.paymentProviderData = paymentProviderData;
    }
    
    public boolean isAutoCapture() {
        return autoCapture;
    }
    
    public void setAutoCapture(boolean autoCapture) {
        this.autoCapture = autoCapture;
    }
}