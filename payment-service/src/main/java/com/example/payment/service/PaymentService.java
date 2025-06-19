package com.example.payment.service;

import com.example.payment.dto.CreatePaymentRequest;
import com.example.payment.dto.CreateRefundRequest;
import com.example.payment.dto.ExecutePaymentRequest;
import com.example.payment.entity.Payment;
import com.example.payment.entity.PaymentMethodType;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.entity.Refund;
import com.example.payment.entity.RefundStatus;
import com.example.payment.exception.InvalidPaymentStatusException;
import com.example.payment.exception.PaymentNotFoundException;
import com.example.payment.exception.PaymentProviderException;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.RefundRepository;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.exception.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private RefundRepository refundRepository;
    
    @Autowired
    private PaymentProviderService paymentProviderService;
    
    public Payment createPayment(CreatePaymentRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            String paymentId = UUID.randomUUID().toString();
            
            Payment payment = new Payment(
                paymentId,
                request.getOrderId(),
                request.getCustomerId(),
                request.getAmount(),
                request.getCurrency(),
                request.getPaymentMethodId(),
                request.getPaymentProvider()
            );
            
            // Set payment method type based on provider (simplified)
            payment.setPaymentMethodTypeEnum(PaymentMethodType.CREDIT_CARD);
            
            paymentRepository.save(transaction, payment);
            transaction.commit();
            
            log.info("Created payment: {}", paymentId);
            return payment;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to create payment for order: {}", request.getOrderId(), e);
            throw new RuntimeException("Failed to create payment", e);
        }
    }
    
    public Payment executePayment(String paymentId, ExecutePaymentRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // Get payment
            Optional<Payment> paymentOpt = paymentRepository.findById(transaction, paymentId);
            if (paymentOpt.isEmpty()) {
                throw new PaymentNotFoundException("Payment not found: " + paymentId);
            }
            
            Payment payment = paymentOpt.get();
            
            // Check payment status
            if (payment.getPaymentStatusEnum() != PaymentStatus.PENDING) {
                throw new InvalidPaymentStatusException("Payment is not in pending status: " + paymentId);
            }
            
            try {
                // Execute payment through provider (mock implementation)
                PaymentProviderResponse providerResponse = paymentProviderService.executePayment(
                    PaymentProviderRequest.builder()
                        .paymentId(paymentId)
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .paymentMethodId(payment.getPaymentMethodId())
                        .orderId(payment.getOrderId())
                        .customerId(payment.getCustomerId())
                        .providerData(request.getPaymentProviderData())
                        .autoCapture(request.isAutoCapture())
                        .build()
                );
                
                // Update payment with provider response
                if (providerResponse.isSuccess()) {
                    payment.setPaymentStatusEnum(request.isAutoCapture() ? 
                        PaymentStatus.CAPTURED : PaymentStatus.AUTHORIZED);
                    payment.setProviderTransactionId(providerResponse.getTransactionId());
                    if (request.isAutoCapture()) {
                        payment.setProcessedAt(System.currentTimeMillis());
                    }
                } else {
                    payment.setPaymentStatusEnum(PaymentStatus.FAILED);
                    payment.setFailureReason(providerResponse.getFailureReason());
                }
                
            } catch (Exception e) {
                payment.setPaymentStatusEnum(PaymentStatus.FAILED);
                payment.setFailureReason("Payment provider error: " + e.getMessage());
                log.error("Payment provider error for payment: {}", paymentId, e);
            }
            
            payment.setUpdatedAt(System.currentTimeMillis());
            payment.setVersion(payment.getVersion() + 1);
            
            paymentRepository.save(transaction, payment);
            transaction.commit();
            
            log.info("Executed payment: {} with status: {}", paymentId, payment.getPaymentStatus());
            return payment;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to execute payment: {}", paymentId, e);
            throw new RuntimeException("Failed to execute payment", e);
        }
    }
    
    public void cancelPayment(String paymentId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // Get payment
            Optional<Payment> paymentOpt = paymentRepository.findById(transaction, paymentId);
            if (paymentOpt.isEmpty()) {
                throw new PaymentNotFoundException("Payment not found: " + paymentId);
            }
            
            Payment payment = paymentOpt.get();
            
            // Check if payment can be cancelled
            PaymentStatus currentStatus = payment.getPaymentStatusEnum();
            if (currentStatus != PaymentStatus.PENDING && currentStatus != PaymentStatus.AUTHORIZED) {
                throw new InvalidPaymentStatusException("Payment cannot be cancelled in current status: " + currentStatus);
            }
            
            // Cancel through provider if necessary
            if (currentStatus == PaymentStatus.AUTHORIZED) {
                try {
                    paymentProviderService.cancelPayment(payment.getProviderTransactionId());
                } catch (Exception e) {
                    log.warn("Failed to cancel payment with provider: {}", paymentId, e);
                }
            }
            
            payment.setPaymentStatusEnum(PaymentStatus.CANCELLED);
            payment.setUpdatedAt(System.currentTimeMillis());
            payment.setVersion(payment.getVersion() + 1);
            
            paymentRepository.save(transaction, payment);
            transaction.commit();
            
            log.info("Cancelled payment: {}", paymentId);
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to cancel payment: {}", paymentId, e);
            throw new RuntimeException("Failed to cancel payment", e);
        }
    }
    
    public Refund processRefund(String paymentId, CreateRefundRequest request) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            // Get payment
            Optional<Payment> paymentOpt = paymentRepository.findById(transaction, paymentId);
            if (paymentOpt.isEmpty()) {
                throw new PaymentNotFoundException("Payment not found: " + paymentId);
            }
            
            Payment payment = paymentOpt.get();
            
            // Check if payment can be refunded
            if (payment.getPaymentStatusEnum() != PaymentStatus.CAPTURED) {
                throw new InvalidPaymentStatusException("Payment cannot be refunded: " + paymentId);
            }
            
            // Check refund amount
            Long totalRefunded = refundRepository.getTotalRefundedAmount(transaction, paymentId);
            Long availableAmount = payment.getAmount() - totalRefunded;
            
            if (request.getRefundAmount() > availableAmount) {
                throw new IllegalArgumentException("Refund amount exceeds available amount");
            }
            
            // Create refund
            String refundId = UUID.randomUUID().toString();
            Refund refund = new Refund(
                refundId,
                paymentId,
                payment.getOrderId(),
                request.getRefundAmount(),
                request.getCurrency(),
                request.getRefundReason()
            );
            
            try {
                // Process refund through provider (mock implementation)
                RefundProviderResponse providerResponse = paymentProviderService.processRefund(
                    RefundProviderRequest.builder()
                        .originalTransactionId(payment.getProviderTransactionId())
                        .refundAmount(request.getRefundAmount())
                        .currency(request.getCurrency())
                        .reason(request.getRefundReason())
                        .build()
                );
                
                if (providerResponse.isSuccess()) {
                    refund.setRefundStatusEnum(RefundStatus.COMPLETED);
                    refund.setProviderRefundId(providerResponse.getRefundId());
                    refund.setProcessedAt(System.currentTimeMillis());
                } else {
                    refund.setRefundStatusEnum(RefundStatus.FAILED);
                }
                
            } catch (Exception e) {
                refund.setRefundStatusEnum(RefundStatus.FAILED);
                log.error("Refund provider error for payment: {}", paymentId, e);
            }
            
            refund.setUpdatedAt(System.currentTimeMillis());
            refundRepository.save(transaction, refund);
            
            // Update payment status if fully refunded
            if (refund.getRefundStatusEnum() == RefundStatus.COMPLETED) {
                Long newTotalRefunded = totalRefunded + request.getRefundAmount();
                if (newTotalRefunded.equals(payment.getAmount())) {
                    payment.setPaymentStatusEnum(PaymentStatus.REFUNDED);
                } else {
                    payment.setPaymentStatusEnum(PaymentStatus.PARTIALLY_REFUNDED);
                }
                payment.setUpdatedAt(System.currentTimeMillis());
                payment.setVersion(payment.getVersion() + 1);
                paymentRepository.save(transaction, payment);
            }
            
            transaction.commit();
            
            log.info("Processed refund: {} for payment: {}", refundId, paymentId);
            return refund;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to process refund for payment: {}", paymentId, e);
            throw new RuntimeException("Failed to process refund", e);
        }
    }
    
    public Optional<Payment> getPayment(String paymentId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            Optional<Payment> payment = paymentRepository.findById(transaction, paymentId);
            transaction.commit();
            return payment;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to get payment: {}", paymentId, e);
            throw new RuntimeException("Failed to get payment", e);
        }
    }
    
    public List<Payment> getPaymentsByCustomer(String customerId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            List<Payment> payments = paymentRepository.findByCustomerId(transaction, customerId);
            transaction.commit();
            return payments;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to get payments for customer: {}", customerId, e);
            throw new RuntimeException("Failed to get payments for customer", e);
        }
    }
    
    public Optional<Refund> getRefund(String refundId) {
        DistributedTransaction transaction = transactionManager.start();
        try {
            Optional<Refund> refund = refundRepository.findById(transaction, refundId);
            transaction.commit();
            return refund;
        } catch (Exception e) {
            transaction.abort();
            log.error("Failed to get refund: {}", refundId, e);
            throw new RuntimeException("Failed to get refund", e);
        }
    }
}