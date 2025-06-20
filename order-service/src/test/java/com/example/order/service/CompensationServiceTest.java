package com.example.order.service;

import com.example.order.client.InventoryServiceClient;
import com.example.order.client.PaymentServiceClient;
import com.example.order.client.ShippingServiceClient;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CompensationService
 */
@ExtendWith(MockitoExtension.class)
class CompensationServiceTest {
    
    @Mock
    private DistributedTransactionManager transactionManager;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private InventoryServiceClient inventoryServiceClient;
    
    @Mock
    private PaymentServiceClient paymentServiceClient;
    
    @Mock
    private ShippingServiceClient shippingServiceClient;
    
    @Mock
    private DistributedTransaction transaction;
    
    @InjectMocks
    private CompensationService compensationService;
    
    @BeforeEach
    void setUp() throws Exception {
        when(transactionManager.start()).thenReturn(transaction);
    }
    
    @Test
    void compensateOrder_CompletedOrder_CompensatesAllServices() throws Exception {
        // Given
        Order order = createCompletedOrder();
        when(orderRepository.findById(order.getOrderId(), transaction))
            .thenReturn(java.util.Optional.of(order));
        
        // When
        compensationService.compensateOrder(order.getOrderId());
        
        // Then
        verify(orderRepository).findById(order.getOrderId(), transaction);
        verify(inventoryServiceClient).cancelReservation("RES-001");
        verify(paymentServiceClient).refundPayment(eq("PAY-001"), any());
        verify(shippingServiceClient).cancelShipment("SHIP-001");
    }
    
    @Test
    void compensateOrder_PendingOrder_OnlyCompensatesInventory() throws Exception {
        // Given
        Order order = createPendingOrder();
        when(orderRepository.findById(order.getOrderId(), transaction))
            .thenReturn(java.util.Optional.of(order));
        
        // When
        compensationService.compensateOrder(order.getOrderId());
        
        // Then
        verify(orderRepository).findById(order.getOrderId(), transaction);
        verify(inventoryServiceClient).cancelReservation("RES-001");
        verify(paymentServiceClient, never()).refundPayment(any(), any());
        verify(shippingServiceClient, never()).cancelShipment(any());
    }
    
    @Test
    void compensateOrder_PaymentCompletedOrder_CompensatesInventoryAndPayment() throws Exception {
        // Given
        Order order = createPaymentCompletedOrder();
        when(orderRepository.findById(order.getOrderId(), transaction))
            .thenReturn(java.util.Optional.of(order));
        
        // When
        compensationService.compensateOrder(order.getOrderId());
        
        // Then
        verify(orderRepository).findById(order.getOrderId(), transaction);
        verify(inventoryServiceClient).cancelReservation("RES-001");
        verify(paymentServiceClient).refundPayment(eq("PAY-001"), any());
        verify(shippingServiceClient, never()).cancelShipment(any());
    }
    
    @Test
    void compensateOrderAsync_OrderExists_CompensatesOrder() throws Exception {
        // Given
        String orderId = "ORD-001";
        Order order = createCompletedOrder();
        
        when(orderRepository.findById(orderId, transaction)).thenReturn(Optional.of(order));
        
        // When
        compensationService.compensateOrderAsync(orderId);
        
        // Then
        verify(orderRepository).findById(orderId, transaction);
        verify(inventoryServiceClient).cancelReservation("RES-001");
        verify(paymentServiceClient).refundPayment(eq("PAY-001"), any());
        verify(shippingServiceClient).cancelShipment("SHIP-001");
        verify(transaction).commit();
    }
    
    @Test
    void compensateOrderAsync_OrderNotFound_NoCompensation() throws Exception {
        // Given
        String orderId = "ORD-999";
        
        when(orderRepository.findById(orderId, transaction)).thenReturn(Optional.empty());
        
        // When
        compensationService.compensateOrderAsync(orderId);
        
        // Then
        verify(orderRepository).findById(orderId, transaction);
        verify(inventoryServiceClient, never()).cancelReservation(any());
        verify(paymentServiceClient, never()).refundPayment(any(), any());
        verify(shippingServiceClient, never()).cancelShipment(any());
        verify(transaction).commit();
    }
    
    @Test
    void compensateOrderAsync_ServiceException_ContinuesCompensation() throws Exception {
        // Given
        String orderId = "ORD-001";
        Order order = createCompletedOrder();
        
        when(orderRepository.findById(orderId, transaction)).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("Inventory service error")).when(inventoryServiceClient).cancelReservation(any());
        
        // When
        compensationService.compensateOrderAsync(orderId);
        
        // Then
        verify(inventoryServiceClient).cancelReservation("RES-001");
        verify(paymentServiceClient).refundPayment(eq("PAY-001"), any());
        verify(shippingServiceClient).cancelShipment("SHIP-001");
        verify(transaction).commit();
    }
    
    private Order createCompletedOrder() {
        Order order = new Order("ORD-001", "CUST-001");
        order.setStatusEnum(OrderStatus.SHIPPED);
        order.setTotalAmount(new BigDecimal("1500.00"));
        order.setInventoryReservationId("RES-001");
        order.setPaymentId("PAY-001");
        order.setShipmentId("SHIP-001");
        return order;
    }
    
    private Order createPendingOrder() {
        Order order = new Order("ORD-001", "CUST-001");
        order.setStatusEnum(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("1500.00"));
        order.setInventoryReservationId("RES-001");
        return order;
    }
    
    private Order createPaymentCompletedOrder() {
        Order order = new Order("ORD-001", "CUST-001");
        order.setStatusEnum(OrderStatus.PAYMENT_COMPLETED);
        order.setTotalAmount(new BigDecimal("1500.00"));
        order.setInventoryReservationId("RES-001");
        order.setPaymentId("PAY-001");
        return order;
    }
}