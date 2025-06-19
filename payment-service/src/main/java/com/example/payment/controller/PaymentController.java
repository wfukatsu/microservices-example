package com.example.payment.controller;

import com.example.payment.dto.CreatePaymentRequest;
import com.example.payment.dto.CreateRefundRequest;
import com.example.payment.dto.ExecutePaymentRequest;
import com.example.payment.entity.Payment;
import com.example.payment.entity.Refund;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    @PostMapping
    public ResponseEntity<Payment> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }
    
    @PostMapping("/{paymentId}/execute")
    public ResponseEntity<Payment> executePayment(
            @PathVariable String paymentId,
            @RequestBody ExecutePaymentRequest request) {
        Payment payment = paymentService.executePayment(paymentId, request);
        return ResponseEntity.ok(payment);
    }
    
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<Void> cancelPayment(@PathVariable String paymentId) {
        paymentService.cancelPayment(paymentId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(@PathVariable String paymentId) {
        Optional<Payment> payment = paymentService.getPayment(paymentId);
        return payment.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<Payment>> getPayments(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String status) {
        
        if (customerId != null) {
            List<Payment> payments = paymentService.getPaymentsByCustomer(customerId);
            return ResponseEntity.ok(payments);
        }
        
        // For simplicity, just return customer payments if no specific filter
        // In a real implementation, you'd have more sophisticated filtering
        return ResponseEntity.ok(List.of());
    }
    
    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<Refund> createRefund(
            @PathVariable String paymentId,
            @Valid @RequestBody CreateRefundRequest request) {
        Refund refund = paymentService.processRefund(paymentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(refund);
    }
    
    @GetMapping("/{paymentId}/refunds")
    public ResponseEntity<List<Refund>> getPaymentRefunds(@PathVariable String paymentId) {
        // This would require a method in PaymentService to get refunds by payment ID
        // For now, return empty list
        return ResponseEntity.ok(List.of());
    }
    
    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<Refund> getRefund(@PathVariable String refundId) {
        Optional<Refund> refund = paymentService.getRefund(refundId);
        return refund.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }
}