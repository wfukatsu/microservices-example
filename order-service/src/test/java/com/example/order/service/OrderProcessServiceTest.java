package com.example.order.service;

import com.example.order.client.*;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.Order;
import com.example.order.entity.OrderItem;
import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.OrderItemRepository;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessServiceTest {
    
    @Mock
    private DistributedTransactionManager transactionManager;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderItemRepository orderItemRepository;
    
    @Mock
    private InventoryServiceClient inventoryServiceClient;
    
    @Mock
    private PaymentServiceClient paymentServiceClient;
    
    @Mock
    private ShippingServiceClient shippingServiceClient;
    
    @Mock
    private DistributedTransaction transaction;
    
    @InjectMocks
    private OrderProcessService orderProcessService;
    
    @BeforeEach
    void setUp() {
        when(transactionManager.start()).thenReturn(transaction);
    }
    
    @Test
    void createOrder_Success() throws Exception {
        // Given
        CreateOrderRequest request = createTestOrderRequest();
        
        InventoryServiceClient.InventoryReservationResponse inventoryResponse = createInventoryResponse();
        PaymentServiceClient.PaymentResponse paymentResponse = createPaymentResponse();
        ShippingServiceClient.ShipmentResponse shipmentResponse = createShipmentResponse();
        
        when(inventoryServiceClient.reserveInventory(any())).thenReturn(inventoryResponse);
        when(paymentServiceClient.processPayment(any())).thenReturn(paymentResponse);
        when(shippingServiceClient.createShipment(any())).thenReturn(shipmentResponse);
        
        // When
        OrderResponse result = orderProcessService.createOrder(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo("CUST-001");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(result.getInventoryReservationId()).isEqualTo("RES-001");
        assertThat(result.getPaymentId()).isEqualTo("PAY-001");
        assertThat(result.getShipmentId()).isEqualTo("SHIP-001");
        
        verify(orderRepository, times(5)).save(eq(transaction), any(Order.class));
        verify(orderItemRepository).saveAll(eq(transaction), any());
        verify(inventoryServiceClient).confirmReservation("RES-001");
        verify(transaction).commit();
    }
    
    @Test
    void createOrder_InventoryFailure_CompensationExecuted() throws Exception {
        // Given
        CreateOrderRequest request = createTestOrderRequest();
        
        when(inventoryServiceClient.reserveInventory(any()))
            .thenThrow(new RuntimeException("Inventory service unavailable"));
        
        // When & Then
        assertThatThrownBy(() -> orderProcessService.createOrder(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to create order");
        
        verify(transaction).abort();
        verify(inventoryServiceClient, never()).confirmReservation(any());
        verify(paymentServiceClient, never()).processPayment(any());
        verify(shippingServiceClient, never()).createShipment(any());
    }
    
    @Test
    void createOrder_PaymentFailure_CompensationExecuted() throws Exception {
        // Given
        CreateOrderRequest request = createTestOrderRequest();
        
        InventoryServiceClient.InventoryReservationResponse inventoryResponse = createInventoryResponse();
        when(inventoryServiceClient.reserveInventory(any())).thenReturn(inventoryResponse);
        when(paymentServiceClient.processPayment(any()))
            .thenThrow(new RuntimeException("Payment failed"));
        
        // When & Then
        assertThatThrownBy(() -> orderProcessService.createOrder(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to create order");
        
        verify(inventoryServiceClient).cancelReservation("RES-001");
        verify(transaction).abort();
        verify(shippingServiceClient, never()).createShipment(any());
    }
    
    @Test
    void createOrder_ShippingFailure_CompensationExecuted() throws Exception {
        // Given
        CreateOrderRequest request = createTestOrderRequest();
        
        InventoryServiceClient.InventoryReservationResponse inventoryResponse = createInventoryResponse();
        PaymentServiceClient.PaymentResponse paymentResponse = createPaymentResponse();
        
        when(inventoryServiceClient.reserveInventory(any())).thenReturn(inventoryResponse);
        when(paymentServiceClient.processPayment(any())).thenReturn(paymentResponse);
        when(shippingServiceClient.createShipment(any()))
            .thenThrow(new RuntimeException("Shipping failed"));
        
        // When & Then
        assertThatThrownBy(() -> orderProcessService.createOrder(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to create order");
        
        verify(inventoryServiceClient).cancelReservation("RES-001");
        verify(paymentServiceClient).refundPayment(eq("PAY-001"), any());
        verify(transaction).abort();
    }
    
    @Test
    void getOrder_Success() throws Exception {
        // Given
        String orderId = "ORD-001";
        Order order = new Order(orderId, "CUST-001", OrderStatus.SHIPPED);
        List<OrderItem> orderItems = List.of(
            new OrderItem(orderId, "PROD-001", "Test Product", 1, new BigDecimal("1500.00"))
        );
        
        when(orderRepository.findById(transaction, orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(transaction, orderId)).thenReturn(orderItems);
        
        // When
        Optional<OrderResponse> result = orderProcessService.getOrder(orderId);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo(orderId);
        assertThat(result.get().getItems()).hasSize(1);
        
        verify(transaction).commit();
    }
    
    @Test
    void getOrder_NotFound() throws Exception {
        // Given
        String orderId = "ORD-999";
        when(orderRepository.findById(transaction, orderId)).thenReturn(Optional.empty());
        
        // When
        Optional<OrderResponse> result = orderProcessService.getOrder(orderId);
        
        // Then
        assertThat(result).isEmpty();
        verify(transaction).commit();
    }
    
    @Test
    void cancelOrder_Success() throws Exception {
        // Given
        String orderId = "ORD-001";
        Order order = new Order(orderId, "CUST-001", OrderStatus.PAYMENT_COMPLETED);
        order.setInventoryReservationId("RES-001");
        order.setPaymentId("PAY-001");
        
        when(orderRepository.findById(transaction, orderId)).thenReturn(Optional.of(order));
        
        // When
        orderProcessService.cancelOrder(orderId);
        
        // Then
        verify(inventoryServiceClient).cancelReservation("RES-001");
        verify(paymentServiceClient).refundPayment(eq("PAY-001"), any());
        verify(orderRepository).save(eq(transaction), any(Order.class));
        verify(transaction).commit();
    }
    
    @Test
    void cancelOrder_AlreadyTerminal_ThrowsException() throws Exception {
        // Given
        String orderId = "ORD-001";
        Order order = new Order(orderId, "CUST-001", OrderStatus.DELIVERED);
        
        when(orderRepository.findById(transaction, orderId)).thenReturn(Optional.of(order));
        
        // When & Then
        assertThatThrownBy(() -> orderProcessService.cancelOrder(orderId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Cannot cancel order in status");
        
        verify(transaction).abort();
    }
    
    @Test
    void getOrdersByCustomer_Success() throws Exception {
        // Given
        String customerId = "CUST-001";
        List<Order> orders = List.of(
            new Order("ORD-001", customerId, OrderStatus.SHIPPED),
            new Order("ORD-002", customerId, OrderStatus.PENDING)
        );
        
        when(orderRepository.findByCustomerId(transaction, customerId)).thenReturn(orders);
        when(orderItemRepository.findByOrderId(transaction, "ORD-001")).thenReturn(new ArrayList<>());
        when(orderItemRepository.findByOrderId(transaction, "ORD-002")).thenReturn(new ArrayList<>());
        
        // When
        List<OrderResponse> result = orderProcessService.getOrdersByCustomer(customerId);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOrderId()).isEqualTo("ORD-001");
        assertThat(result.get(1).getOrderId()).isEqualTo("ORD-002");
        
        verify(transaction).commit();
    }
    
    private CreateOrderRequest createTestOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("CUST-001");
        request.setNotes("Test order");
        
        // Items
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId("PROD-001");
        item.setQuantity(1);
        request.setItems(List.of(item));
        
        // Payment details
        CreateOrderRequest.PaymentMethodDetails paymentDetails = new CreateOrderRequest.PaymentMethodDetails();
        paymentDetails.setPaymentMethod("CREDIT_CARD");
        paymentDetails.setCardNumber("4111111111111111");
        paymentDetails.setExpiryMonth("12");
        paymentDetails.setExpiryYear("2025");
        paymentDetails.setCvv("123");
        paymentDetails.setCardholderName("Test User");
        request.setPaymentMethodDetails(paymentDetails);
        
        // Shipping info
        CreateOrderRequest.ShippingInfo shippingInfo = new CreateOrderRequest.ShippingInfo();
        shippingInfo.setShippingMethod("STANDARD");
        shippingInfo.setCarrier("YAMATO");
        
        CreateOrderRequest.ShippingInfo.RecipientInfo recipientInfo = new CreateOrderRequest.ShippingInfo.RecipientInfo();
        recipientInfo.setName("田中太郎");
        recipientInfo.setPhone("090-1234-5678");
        recipientInfo.setAddress("東京都渋谷区渋谷1-1-1");
        recipientInfo.setCity("渋谷区");
        recipientInfo.setState("東京都");
        recipientInfo.setPostalCode("150-0002");
        recipientInfo.setCountry("JP");
        shippingInfo.setRecipientInfo(recipientInfo);
        
        request.setShippingInfo(shippingInfo);
        
        return request;
    }
    
    private InventoryServiceClient.InventoryReservationResponse createInventoryResponse() {
        InventoryServiceClient.InventoryReservationResponse response = new InventoryServiceClient.InventoryReservationResponse();
        response.setReservationId("RES-001");
        response.setOrderId("ORD-001");
        response.setCustomerId("CUST-001");
        response.setStatus("RESERVED");
        response.setExpiresAt(LocalDateTime.now().plusHours(24));
        response.setCreatedAt(LocalDateTime.now());
        
        InventoryServiceClient.ReservedItem item = new InventoryServiceClient.ReservedItem();
        item.setProductId("PROD-001");
        item.setProductName("Test Product");
        item.setReservedQuantity(1);
        item.setUnitPrice(new BigDecimal("1500.00"));
        response.setItems(List.of(item));
        
        return response;
    }
    
    private PaymentServiceClient.PaymentResponse createPaymentResponse() {
        PaymentServiceClient.PaymentResponse response = new PaymentServiceClient.PaymentResponse();
        response.setPaymentId("PAY-001");
        response.setOrderId("ORD-001");
        response.setCustomerId("CUST-001");
        response.setAmount(new BigDecimal("1500.00"));
        response.setCurrency("JPY");
        response.setStatus("COMPLETED");
        response.setPaymentMethod("CREDIT_CARD");
        response.setTransactionId("TXN-001");
        response.setAuthorizationCode("AUTH-001");
        response.setProcessedAt(LocalDateTime.now());
        
        return response;
    }
    
    private ShippingServiceClient.ShipmentResponse createShipmentResponse() {
        ShippingServiceClient.ShipmentResponse response = new ShippingServiceClient.ShipmentResponse();
        response.setShipmentId("SHIP-001");
        response.setOrderId("ORD-001");
        response.setCustomerId("CUST-001");
        response.setStatus("PROCESSING");
        response.setCarrier("YAMATO");
        response.setTrackingNumber("ST123456789012");
        response.setShippingMethod("STANDARD");
        response.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(3));
        response.setCreatedAt(LocalDateTime.now());
        
        return response;
    }
}