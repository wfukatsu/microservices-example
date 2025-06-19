package com.example.payment.controller;

import com.example.payment.dto.ProcessPaymentRequest;
import com.example.payment.dto.RefundRequest;
import com.example.payment.entity.Payment;
import com.example.payment.entity.Refund;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payments/process")
@CrossOrigin(origins = "*")
public class PaymentProcessController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessController.class);
    
    @Autowired
    private PaymentService paymentService;

    @PostMapping("/quick-payment")
    public ResponseEntity<Payment> quickPayment(@Valid @RequestBody QuickPaymentRequest request) {
        logger.info("Received quick payment request for customer: {}", request.getCustomerId());
        
        try {
            ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest();
            paymentRequest.setOrderId("QUICK-PAY-" + System.currentTimeMillis());
            paymentRequest.setCustomerId(request.getCustomerId());
            paymentRequest.setAmount(request.getAmount());
            paymentRequest.setCurrency(request.getCurrency() != null ? request.getCurrency() : "JPY");
            paymentRequest.setPaymentMethod(request.getPaymentMethod());
            paymentRequest.setDescription("Quick payment for customer: " + request.getCustomerId());
            paymentRequest.setPaymentMethodDetails(request.getPaymentMethodDetails());
            
            Payment payment = paymentService.processPayment(paymentRequest);
            
            logger.info("Quick payment processed successfully: {}", payment.getPaymentId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
            
        } catch (Exception e) {
            logger.error("Failed to process quick payment for customer: {}, error: {}", 
                request.getCustomerId(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/authorize-and-capture")
    public ResponseEntity<Payment> authorizeAndCapture(@Valid @RequestBody ProcessPaymentRequest request) {
        logger.info("Received authorize and capture request for order: {}", request.getOrderId());
        
        try {
            // Process payment (which includes authorization and capture)
            Payment payment = paymentService.processPayment(request);
            
            logger.info("Payment authorized and captured successfully for order: {}, payment: {}", 
                request.getOrderId(), payment.getPaymentId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
            
        } catch (Exception e) {
            logger.error("Failed to authorize and capture payment for order: {}, error: {}", 
                request.getOrderId(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/bulk-refund")
    public ResponseEntity<BulkRefundResponse> bulkRefund(@Valid @RequestBody BulkRefundRequest request) {
        logger.info("Received bulk refund request for {} payments", request.getRefundRequests().size());
        
        BulkRefundResponse response = new BulkRefundResponse();
        
        for (BulkRefundRequest.RefundRequestItem refundItem : request.getRefundRequests()) {
            try {
                RefundRequest refundRequest = new RefundRequest();
                refundRequest.setAmount(refundItem.getAmount());
                refundRequest.setReason(refundItem.getReason());
                
                Refund refund = paymentService.refundPayment(refundItem.getPaymentId(), refundRequest);
                
                BulkRefundResponse.RefundResult result = new BulkRefundResponse.RefundResult();
                result.setPaymentId(refundItem.getPaymentId());
                result.setRefundId(refund.getRefundId());
                result.setRefundAmount(refund.getRefundAmount());
                result.setStatus(refund.getStatus());
                response.getSuccessfulRefunds().add(result);
                
                logger.debug("Refunded payment: {}", refundItem.getPaymentId());
                
            } catch (Exception e) {
                logger.warn("Failed to refund payment: {}, error: {}", refundItem.getPaymentId(), e.getMessage());
                response.getFailedRefunds().put(refundItem.getPaymentId(), e.getMessage());
            }
        }
        
        logger.info("Bulk refund completed: {} successful, {} failed", 
            response.getSuccessfulRefunds().size(), 
            response.getFailedRefunds().size());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-and-pay")
    public ResponseEntity<PaymentValidationResponse> validateAndPay(@Valid @RequestBody ProcessPaymentRequest request) {
        logger.info("Received validate and pay request for order: {}", request.getOrderId());
        
        try {
            // Basic validation
            PaymentValidationResponse validationResponse = new PaymentValidationResponse();
            
            // Validate amount
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                validationResponse.setValid(false);
                validationResponse.setErrorMessage("Amount must be greater than zero");
                return ResponseEntity.badRequest().body(validationResponse);
            }
            
            // Validate payment method
            if (request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
                validationResponse.setValid(false);
                validationResponse.setErrorMessage("Payment method is required");
                return ResponseEntity.badRequest().body(validationResponse);
            }
            
            // Process payment if validation passes
            Payment payment = paymentService.processPayment(request);
            
            validationResponse.setValid(true);
            validationResponse.setPayment(payment);
            
            logger.info("Payment validated and processed successfully for order: {}, payment: {}", 
                request.getOrderId(), payment.getPaymentId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(validationResponse);
            
        } catch (Exception e) {
            logger.error("Failed to validate and process payment for order: {}, error: {}", 
                request.getOrderId(), e.getMessage());
            
            PaymentValidationResponse errorResponse = new PaymentValidationResponse();
            errorResponse.setValid(false);
            errorResponse.setErrorMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // DTOs
    public static class QuickPaymentRequest {
        @jakarta.validation.constraints.NotBlank(message = "Customer ID is required")
        private String customerId;
        
        @jakarta.validation.constraints.NotNull(message = "Amount is required")
        @jakarta.validation.constraints.DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amount;
        
        private String currency;
        
        @jakarta.validation.constraints.NotBlank(message = "Payment method is required")
        private String paymentMethod;
        
        private ProcessPaymentRequest.PaymentMethodDetails paymentMethodDetails;

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public ProcessPaymentRequest.PaymentMethodDetails getPaymentMethodDetails() { return paymentMethodDetails; }
        public void setPaymentMethodDetails(ProcessPaymentRequest.PaymentMethodDetails paymentMethodDetails) { this.paymentMethodDetails = paymentMethodDetails; }
    }

    public static class BulkRefundRequest {
        @jakarta.validation.constraints.NotEmpty(message = "Refund requests are required")
        private java.util.List<RefundRequestItem> refundRequests;

        public java.util.List<RefundRequestItem> getRefundRequests() { return refundRequests; }
        public void setRefundRequests(java.util.List<RefundRequestItem> refundRequests) { this.refundRequests = refundRequests; }

        public static class RefundRequestItem {
            @jakarta.validation.constraints.NotBlank(message = "Payment ID is required")
            private String paymentId;
            
            private BigDecimal amount;
            private String reason;

            public String getPaymentId() { return paymentId; }
            public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
            public BigDecimal getAmount() { return amount; }
            public void setAmount(BigDecimal amount) { this.amount = amount; }
            public String getReason() { return reason; }
            public void setReason(String reason) { this.reason = reason; }
        }
    }

    public static class BulkRefundResponse {
        private java.util.List<RefundResult> successfulRefunds = new java.util.ArrayList<>();
        private java.util.Map<String, String> failedRefunds = new java.util.HashMap<>();

        public java.util.List<RefundResult> getSuccessfulRefunds() { return successfulRefunds; }
        public void setSuccessfulRefunds(java.util.List<RefundResult> successfulRefunds) { this.successfulRefunds = successfulRefunds; }
        public java.util.Map<String, String> getFailedRefunds() { return failedRefunds; }
        public void setFailedRefunds(java.util.Map<String, String> failedRefunds) { this.failedRefunds = failedRefunds; }

        public static class RefundResult {
            private String paymentId;
            private String refundId;
            private BigDecimal refundAmount;
            private String status;

            public String getPaymentId() { return paymentId; }
            public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
            public String getRefundId() { return refundId; }
            public void setRefundId(String refundId) { this.refundId = refundId; }
            public BigDecimal getRefundAmount() { return refundAmount; }
            public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
            public String getStatus() { return status; }
            public void setStatus(String status) { this.status = status; }
        }
    }

    public static class PaymentValidationResponse {
        private boolean valid;
        private String errorMessage;
        private Payment payment;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Payment getPayment() { return payment; }
        public void setPayment(Payment payment) { this.payment = payment; }
    }
}