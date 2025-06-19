package com.example.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class MockPaymentProviderService implements PaymentProviderService {
    
    private static final Logger log = LoggerFactory.getLogger(MockPaymentProviderService.class);
    
    @Override
    public PaymentProviderResponse executePayment(PaymentProviderRequest request) {
        log.info("Mock payment execution for amount: {} {}", request.getAmount(), request.getCurrency());
        
        // Simulate payment processing
        try {
            Thread.sleep(100); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock success/failure based on amount (for testing)
        boolean success = request.getAmount() < 1000000; // Fail if amount >= 1,000,000
        
        if (success) {
            return PaymentProviderResponse.builder()
                .success(true)
                .transactionId("mock_tx_" + UUID.randomUUID().toString())
                .build();
        } else {
            return PaymentProviderResponse.builder()
                .success(false)
                .failureReason("Amount exceeds limit")
                .build();
        }
    }
    
    @Override
    public void cancelPayment(String transactionId) {
        log.info("Mock payment cancellation for transaction: {}", transactionId);
        
        // Simulate cancellation processing
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public RefundProviderResponse processRefund(RefundProviderRequest request) {
        log.info("Mock refund processing for amount: {} {}", request.getRefundAmount(), request.getCurrency());
        
        // Simulate refund processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock success (always succeed for simplicity)
        return RefundProviderResponse.builder()
            .success(true)
            .refundId("mock_refund_" + UUID.randomUUID().toString())
            .build();
    }
}