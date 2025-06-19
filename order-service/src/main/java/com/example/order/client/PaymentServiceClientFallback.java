package com.example.order.client;

import org.springframework.stereotype.Component;

@Component
public class PaymentServiceClientFallback implements PaymentServiceClient {

    @Override
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        throw new RuntimeException("Payment service is currently unavailable");
    }

    @Override
    public RefundResponse refundPayment(String paymentId, RefundRequest request) {
        throw new RuntimeException("Payment service is currently unavailable");
    }

    @Override
    public PaymentResponse getPayment(String paymentId) {
        throw new RuntimeException("Payment service is currently unavailable");
    }
}