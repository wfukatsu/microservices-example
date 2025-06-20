package com.example.order.service;

import com.example.order.client.*;
import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.exception.transaction.AbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for compensating failed order transactions
 */
@Service
public class CompensationService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompensationService.class);
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private InventoryServiceClient inventoryServiceClient;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Autowired
    private ShippingServiceClient shippingServiceClient;
    
    /**
     * Compensate a failed order by rolling back external service operations
     */
    public void compensateOrder(String orderId) throws ExecutionException, TransactionException, AbortException {
        logger.info("Starting compensation for order: {}", orderId);
        
        DistributedTransaction transaction = transactionManager.start();
        
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId, transaction);
            transaction.commit();
            
            if (orderOpt.isEmpty()) {
                logger.warn("Order not found for compensation: {}", orderId);
                return;
            }
            
            Order order = orderOpt.get();
            
            // Compensate in reverse order of operations
            compensateShipment(order);
            compensatePayment(order);
            compensateInventoryReservation(order);
            
            logger.info("Compensation completed for order: {}", orderId);
            
        } catch (Exception e) {
            logger.error("Compensation failed for order: {}", orderId, e);
            transaction.abort();
            // Schedule retry for failed compensation
            scheduleCompensationRetry(orderId);
        }
    }
    
    /**
     * Asynchronous compensation for better performance
     */
    @Async
    public CompletableFuture<Void> compensateOrderAsync(String orderId) {
        try {
            compensateOrder(orderId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Async compensation failed for order: {}", orderId, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    private void compensateShipment(Order order) {
        if (order.getShipmentId() != null) {
            try {
                shippingServiceClient.cancelShipment(order.getShipmentId());
                logger.info("Cancelled shipment: {} for order: {}", order.getShipmentId(), order.getOrderId());
            } catch (Exception e) {
                logger.warn("Failed to cancel shipment: {} for order: {}, error: {}", 
                    order.getShipmentId(), order.getOrderId(), e.getMessage());
            }
        }
    }
    
    private void compensatePayment(Order order) {
        if (order.getPaymentId() != null) {
            try {
                PaymentServiceClient.RefundRequest refundRequest = 
                    new PaymentServiceClient.RefundRequest(order.getTotalAmount(), "Order cancellation");
                paymentServiceClient.refundPayment(order.getPaymentId(), refundRequest);
                logger.info("Refunded payment: {} for order: {}", order.getPaymentId(), order.getOrderId());
            } catch (Exception e) {
                logger.warn("Failed to refund payment: {} for order: {}, error: {}", 
                    order.getPaymentId(), order.getOrderId(), e.getMessage());
            }
        }
    }
    
    private void compensateInventoryReservation(Order order) {
        if (order.getInventoryReservationId() != null) {
            try {
                inventoryServiceClient.cancelReservation(order.getInventoryReservationId());
                logger.info("Cancelled inventory reservation: {} for order: {}", 
                    order.getInventoryReservationId(), order.getOrderId());
            } catch (Exception e) {
                logger.warn("Failed to cancel inventory reservation: {} for order: {}, error: {}", 
                    order.getInventoryReservationId(), order.getOrderId(), e.getMessage());
            }
        }
    }
    
    private void scheduleCompensationRetry(String orderId) {
        // In a production system, this would typically be implemented with:
        // - Dead letter queue
        // - Scheduled job
        // - Circuit breaker pattern
        logger.warn("Scheduling compensation retry for order: {} (not implemented in demo)", orderId);
    }
}