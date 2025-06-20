package com.example.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@FeignClient(
    name = "payment-service",
    url = "${services.payment.url}",
    fallback = PaymentServiceClientFallback.class
)
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payments/process")
    PaymentResponse processPayment(@RequestBody ProcessPaymentRequest request);

    @PostMapping("/api/v1/payments/{paymentId}/refund")
    RefundResponse refundPayment(@PathVariable String paymentId, @RequestBody RefundRequest request);

    @GetMapping("/api/v1/payments/{paymentId}")
    PaymentResponse getPayment(@PathVariable String paymentId);

    // DTOs
    class ProcessPaymentRequest {
        private String orderId;
        private String customerId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private PaymentMethodDetails paymentMethodDetails;
        private String description;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public PaymentMethodDetails getPaymentMethodDetails() { return paymentMethodDetails; }
        public void setPaymentMethodDetails(PaymentMethodDetails paymentMethodDetails) { this.paymentMethodDetails = paymentMethodDetails; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    class PaymentMethodDetails {
        // ✅ Security: Use tokenized payment data instead of raw card details
        private String paymentToken;
        private String last4Digits;
        private String cardBrand;
        private String cardholderName;

        public String getPaymentToken() { return paymentToken; }
        public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }
        public String getLast4Digits() { return last4Digits; }
        public void setLast4Digits(String last4Digits) { this.last4Digits = last4Digits; }
        public String getCardBrand() { return cardBrand; }
        public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }
        public String getCardholderName() { return cardholderName; }
        public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }
    }

    class PaymentResponse {
        private String paymentId;
        private String orderId;
        private String customerId;
        private BigDecimal amount;
        private String currency;
        private String status;
        private String paymentMethod;
        private String transactionId;
        private String authorizationCode;
        private LocalDateTime processedAt;
        private String errorMessage;

        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getAuthorizationCode() { return authorizationCode; }
        public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
        public LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    class RefundRequest {
        private BigDecimal amount;
        private String reason;

        public RefundRequest() {}

        public RefundRequest(BigDecimal amount, String reason) {
            this.amount = amount;
            this.reason = reason;
        }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    class RefundResponse {
        private String refundId;
        private String paymentId;
        private BigDecimal refundAmount;
        private String status;
        private LocalDateTime processedAt;

        public String getRefundId() { return refundId; }
        public void setRefundId(String refundId) { this.refundId = refundId; }
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public BigDecimal getRefundAmount() { return refundAmount; }
        public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    }
}