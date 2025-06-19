package com.example.payment.repository;

import com.example.payment.entity.Payment;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PaymentRepository {
    
    private static final String NAMESPACE = "payment";
    private static final String TABLE_NAME = "payments";
    
    public Optional<Payment> findById(DistributedTransaction transaction, String paymentId) 
            throws TransactionException {
        Get get = Get.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.ofText("payment_id", paymentId))
            .build();
        
        Optional<Result> result = transaction.get(get);
        return result.map(this::mapResultToPayment);
    }
    
    public List<Payment> findByCustomerId(DistributedTransaction transaction, String customerId) 
            throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<Payment> payments = new ArrayList<>();
        for (Result result : results) {
            Payment payment = mapResultToPayment(result);
            if (customerId.equals(payment.getCustomerId())) {
                payments.add(payment);
            }
        }
        return payments;
    }
    
    public List<Payment> findByOrderId(DistributedTransaction transaction, String orderId) 
            throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<Payment> payments = new ArrayList<>();
        for (Result result : results) {
            Payment payment = mapResultToPayment(result);
            if (orderId.equals(payment.getOrderId())) {
                payments.add(payment);
            }
        }
        return payments;
    }
    
    public List<Payment> findByStatus(DistributedTransaction transaction, String status) 
            throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<Payment> payments = new ArrayList<>();
        for (Result result : results) {
            Payment payment = mapResultToPayment(result);
            if (status.equals(payment.getPaymentStatus())) {
                payments.add(payment);
            }
        }
        return payments;
    }
    
    public void save(DistributedTransaction transaction, Payment payment) throws TransactionException {
        Put.Builder putBuilder = Put.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.ofText("payment_id", payment.getPaymentId()))
            .textValue("order_id", payment.getOrderId())
            .textValue("customer_id", payment.getCustomerId())
            .bigIntValue("amount", payment.getAmount())
            .textValue("currency", payment.getCurrency())
            .textValue("payment_method_id", payment.getPaymentMethodId())
            .textValue("payment_status", payment.getPaymentStatus())
            .textValue("payment_provider", payment.getPaymentProvider())
            .bigIntValue("created_at", payment.getCreatedAt())
            .bigIntValue("updated_at", payment.getUpdatedAt())
            .intValue("version", payment.getVersion());
        
        if (payment.getPaymentMethodType() != null) {
            putBuilder.textValue("payment_method_type", payment.getPaymentMethodType());
        }
        if (payment.getProviderTransactionId() != null) {
            putBuilder.textValue("provider_transaction_id", payment.getProviderTransactionId());
        }
        if (payment.getFailureReason() != null) {
            putBuilder.textValue("failure_reason", payment.getFailureReason());
        }
        if (payment.getProcessedAt() != null) {
            putBuilder.bigIntValue("processed_at", payment.getProcessedAt());
        }
        
        transaction.put(putBuilder.build());
    }
    
    public boolean existsById(DistributedTransaction transaction, String paymentId) 
            throws TransactionException {
        return findById(transaction, paymentId).isPresent();
    }
    
    public long countFailedPayments(DistributedTransaction transaction) throws TransactionException {
        return findByStatus(transaction, "FAILED").size();
    }
    
    public long countSuccessfulPayments(DistributedTransaction transaction) throws TransactionException {
        return findByStatus(transaction, "CAPTURED").size();
    }
    
    private Payment mapResultToPayment(Result result) {
        Payment payment = new Payment();
        
        result.getValue("payment_id").ifPresent(v -> payment.setPaymentId(((TextValue) v).get()));
        result.getValue("order_id").ifPresent(v -> payment.setOrderId(((TextValue) v).get()));
        result.getValue("customer_id").ifPresent(v -> payment.setCustomerId(((TextValue) v).get()));
        result.getValue("amount").ifPresent(v -> payment.setAmount(v.getAsLong()));
        result.getValue("currency").ifPresent(v -> payment.setCurrency(((TextValue) v).get()));
        result.getValue("payment_method_type").ifPresent(v -> payment.setPaymentMethodType(((TextValue) v).get()));
        result.getValue("payment_method_id").ifPresent(v -> payment.setPaymentMethodId(((TextValue) v).get()));
        result.getValue("payment_status").ifPresent(v -> payment.setPaymentStatus(((TextValue) v).get()));
        result.getValue("payment_provider").ifPresent(v -> payment.setPaymentProvider(((TextValue) v).get()));
        result.getValue("provider_transaction_id").ifPresent(v -> payment.setProviderTransactionId(((TextValue) v).get()));
        result.getValue("failure_reason").ifPresent(v -> payment.setFailureReason(((TextValue) v).get()));
        result.getValue("processed_at").ifPresent(v -> payment.setProcessedAt(v.getAsLong()));
        result.getValue("created_at").ifPresent(v -> payment.setCreatedAt(v.getAsLong()));
        result.getValue("updated_at").ifPresent(v -> payment.setUpdatedAt(v.getAsLong()));
        result.getValue("version").ifPresent(v -> payment.setVersion(v.getAsInt()));
        
        return payment;
    }
}