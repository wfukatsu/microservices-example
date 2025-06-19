package com.example.payment.repository;

import com.example.payment.entity.Refund;
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
public class RefundRepository {
    
    private static final String NAMESPACE = "payment";
    private static final String TABLE_NAME = "refunds";
    
    public Optional<Refund> findById(DistributedTransaction transaction, String refundId) 
            throws TransactionException {
        Get get = Get.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.ofText("refund_id", refundId))
            .build();
        
        Optional<Result> result = transaction.get(get);
        return result.map(this::mapResultToRefund);
    }
    
    public List<Refund> findByPaymentId(DistributedTransaction transaction, String paymentId) 
            throws TransactionException {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .build();
        
        List<Result> results = transaction.scan(scan);
        List<Refund> refunds = new ArrayList<>();
        for (Result result : results) {
            Refund refund = mapResultToRefund(result);
            if (paymentId.equals(refund.getPaymentId())) {
                refunds.add(refund);
            }
        }
        return refunds;
    }
    
    public void save(DistributedTransaction transaction, Refund refund) throws TransactionException {
        Put.Builder putBuilder = Put.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.ofText("refund_id", refund.getRefundId()))
            .textValue("payment_id", refund.getPaymentId())
            .textValue("order_id", refund.getOrderId())
            .bigIntValue("refund_amount", refund.getRefundAmount())
            .textValue("currency", refund.getCurrency())
            .textValue("refund_reason", refund.getRefundReason())
            .textValue("refund_status", refund.getRefundStatus())
            .bigIntValue("created_at", refund.getCreatedAt())
            .bigIntValue("updated_at", refund.getUpdatedAt());
        
        if (refund.getProviderRefundId() != null) {
            putBuilder.textValue("provider_refund_id", refund.getProviderRefundId());
        }
        if (refund.getProcessedAt() != null) {
            putBuilder.bigIntValue("processed_at", refund.getProcessedAt());
        }
        
        transaction.put(putBuilder.build());
    }
    
    public Long getTotalRefundedAmount(DistributedTransaction transaction, String paymentId) 
            throws TransactionException {
        List<Refund> refunds = findByPaymentId(transaction, paymentId);
        return refunds.stream()
                .filter(refund -> "COMPLETED".equals(refund.getRefundStatus()))
                .mapToLong(Refund::getRefundAmount)
                .sum();
    }
    
    public boolean existsById(DistributedTransaction transaction, String refundId) 
            throws TransactionException {
        return findById(transaction, refundId).isPresent();
    }
    
    private Refund mapResultToRefund(Result result) {
        Refund refund = new Refund();
        
        result.getValue("refund_id").ifPresent(v -> refund.setRefundId(((TextValue) v).get()));
        result.getValue("payment_id").ifPresent(v -> refund.setPaymentId(((TextValue) v).get()));
        result.getValue("order_id").ifPresent(v -> refund.setOrderId(((TextValue) v).get()));
        result.getValue("refund_amount").ifPresent(v -> refund.setRefundAmount(v.getAsLong()));
        result.getValue("currency").ifPresent(v -> refund.setCurrency(((TextValue) v).get()));
        result.getValue("refund_reason").ifPresent(v -> refund.setRefundReason(((TextValue) v).get()));
        result.getValue("refund_status").ifPresent(v -> refund.setRefundStatus(((TextValue) v).get()));
        result.getValue("provider_refund_id").ifPresent(v -> refund.setProviderRefundId(((TextValue) v).get()));
        result.getValue("processed_at").ifPresent(v -> refund.setProcessedAt(v.getAsLong()));
        result.getValue("created_at").ifPresent(v -> refund.setCreatedAt(v.getAsLong()));
        result.getValue("updated_at").ifPresent(v -> refund.setUpdatedAt(v.getAsLong()));
        
        return refund;
    }
}