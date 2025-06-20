package com.example.order.service;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.Order;
import com.example.order.entity.OrderItem;
import com.example.order.entity.OrderStatus;
import com.example.order.exception.OrderErrorCode;
import com.example.order.exception.OrderProcessingException;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.OrderItemRepository;
import com.example.order.security.SensitiveDataFilter;
import com.example.order.client.InventoryServiceClient;
import com.example.order.client.PaymentServiceClient;
import com.example.order.client.ShippingServiceClient;
import com.example.order.service.CompensationService;
import com.example.order.service.CacheService;
import com.example.order.service.MetricsService;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;

/**
 * Updated unit tests for the refactored OrderProcessService
 */
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
    private CompensationService compensationService;
    
    @Mock
    private CacheService cacheService;
    
    @Mock
    private MetricsService metricsService;
    
    @Mock
    private SensitiveDataFilter sensitiveDataFilter;
    
    @Mock
    private DistributedTransaction transaction;
    
    private OrderProcessService orderProcessService;
    
    @BeforeEach
    void setUp() throws Exception {
        orderProcessService = new OrderProcessService(
            transactionManager,
            orderRepository,
            orderItemRepository,
            inventoryServiceClient,
            paymentServiceClient,
            shippingServiceClient,
            compensationService,
            sensitiveDataFilter,
            cacheService,
            metricsService
        );
    
        lenient().when(transactionManager.start()).thenReturn(transaction);
        lenient().when(sensitiveDataFilter.sanitizeForLogging(any())).thenReturn("sanitized-data");
    }
    
    @Test
    void createOrder_Success() throws Exception {
        // Given
        CreateOrderRequest request = createTestOrderRequest();
        
        // When
        OrderResponse result = orderProcessService.createOrder(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).startsWith("ORD-");
        assertThat(result.getCustomerId()).isEqualTo("CUST-001");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        
        verify(orderRepository).create(any(Order.class), eq(transaction));
        verify(orderItemRepository).create(any(OrderItem.class), eq(transaction));
        verify(transaction).commit();
        verify(sensitiveDataFilter).sanitizeForLogging(request);
    }
    
    @Test
    void createOrder_TransactionFailure_ThrowsOrderProcessingException() throws Exception {
        // Given
        CreateOrderRequest request = createTestOrderRequest();
        RuntimeException transactionException = new RuntimeException("Transaction failed");
        
        doThrow(transactionException).when(orderRepository).create(any(Order.class), eq(transaction));
        
        // When & Then
        assertThatThrownBy(() -> orderProcessService.createOrder(request))
            .isInstanceOf(OrderProcessingException.class)
            .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.SYSTEM_ERROR)
            .hasFieldOrPropertyWithValue("orderId", "UNKNOWN")
            .hasMessageContaining("An unexpected error occurred during order creation");
        
        verify(transaction).abort();
    }
    
    @Test
    void createOrder_UnexpectedError_ThrowsSystemErrorException() throws Exception {
        // Given
        CreateOrderRequest request = createTestOrderRequest();
        RuntimeException unexpectedException = new RuntimeException("Unexpected error");
        
        doThrow(unexpectedException).when(orderItemRepository).create(any(OrderItem.class), eq(transaction));
        
        // When & Then
        assertThatThrownBy(() -> orderProcessService.createOrder(request))
            .isInstanceOf(OrderProcessingException.class)
            .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.SYSTEM_ERROR)
            .hasFieldOrPropertyWithValue("orderId", "UNKNOWN")
            .hasMessageContaining("An unexpected error occurred during order creation");
        
        verify(transaction).abort();
    }
    
    @Test
    void getOrder_Success() throws Exception {
        // Given
        String orderId = "ORD-001";
        Order order = createTestOrder();
        List<OrderItem> orderItems = createTestOrderItems();
        
        when(orderRepository.findById(orderId, transaction)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId, transaction)).thenReturn(orderItems);
        
        // When
        Optional<OrderResponse> result = orderProcessService.getOrder(orderId);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo(orderId);
        assertThat(result.get().getItems()).hasSize(1);
        
        verify(orderRepository).findById(orderId, transaction);
        verify(orderItemRepository).findByOrderId(orderId, transaction);
        verify(transaction, times(1)).commit();
    }
    
    @Test
    void getOrder_NotFound_ReturnsEmpty() throws Exception {
        // Given
        String orderId = "ORD-999";
        when(orderRepository.findById(orderId, transaction)).thenReturn(Optional.empty());
        
        // When
        Optional<OrderResponse> result = orderProcessService.getOrder(orderId);
        
        // Then
        assertThat(result).isEmpty();
        verify(orderRepository).findById(orderId, transaction);
        verify(transaction, times(1)).commit();
    }
    
    @Test
    void cancelOrder_Success() throws Exception {
        // Given
        String orderId = "ORD-001";
        Order order = createTestOrder();
        order.setStatusEnum(OrderStatus.PAYMENT_COMPLETED);
        
        when(orderRepository.findById(orderId, transaction)).thenReturn(Optional.of(order));
        
        // When
        orderProcessService.cancelOrder(orderId);
        
        // Then
        verify(orderRepository).findById(orderId, transaction);
        verify(orderRepository).update(any(Order.class), eq(transaction));
        verify(compensationService).compensateOrderAsync(orderId);
        verify(transaction, times(1)).commit();
    }
    
    @Test
    void cancelOrder_OrderNotFound_ThrowsException() throws Exception {
        // Given
        String orderId = "ORD-999";
        when(orderRepository.findById(orderId, transaction)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> orderProcessService.cancelOrder(orderId))
            .isInstanceOf(OrderProcessingException.class)
            .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.ORDER_NOT_FOUND)
            .hasFieldOrPropertyWithValue("orderId", orderId)
            .hasMessageContaining("Order not found");
        
        verify(orderRepository).findById(orderId, transaction);
        verify(transaction, atLeast(1)).abort();
    }
    
    @Test
    void cancelOrder_TerminalStatus_ThrowsException() throws Exception {
        // Given
        String orderId = "ORD-001";
        Order order = createTestOrder();
        order.setStatusEnum(OrderStatus.DELIVERED); // Terminal status
        
        when(orderRepository.findById(orderId, transaction)).thenReturn(Optional.of(order));
        
        // When & Then
        assertThatThrownBy(() -> orderProcessService.cancelOrder(orderId))
            .isInstanceOf(OrderProcessingException.class)
            .hasFieldOrPropertyWithValue("errorCode", OrderErrorCode.INVALID_REQUEST)
            .hasFieldOrPropertyWithValue("orderId", orderId)
            .hasMessageContaining("Cannot cancel order in status");
        
        verify(orderRepository).findById(orderId, transaction);
        verify(transaction, atLeast(1)).abort();
    }
    
    @Test
    void getOrdersByCustomer_Success() throws Exception {
        // Given
        String customerId = "CUST-001";
        List<Order> orders = List.of(createTestOrder());
        List<OrderItem> orderItems = createTestOrderItems();
        
        when(orderRepository.findByCustomerId(customerId, transaction)).thenReturn(orders);
        when(orderItemRepository.findByOrderIds(any(), eq(transaction))).thenReturn(Map.of("ORD-001", orderItems));
        
        // When
        List<OrderResponse> result = orderProcessService.getOrdersByCustomer(customerId);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(customerId);
        
        verify(orderRepository).findByCustomerId(customerId, transaction);
        verify(orderItemRepository).findByOrderIds(any(), eq(transaction));
        verify(transaction, times(1)).commit();
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
        
        // Secure payment details (tokenized)
        CreateOrderRequest.PaymentMethodDetails paymentDetails = new CreateOrderRequest.PaymentMethodDetails();
        paymentDetails.setPaymentMethod("CREDIT_CARD");
        paymentDetails.setPaymentToken("tok_1234567890abcdef");
        paymentDetails.setLast4Digits("1234");
        paymentDetails.setCardBrand("VISA");
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
    
    private OrderResponse createTestOrderResponse() {
        Order order = createTestOrder();
        List<OrderItem> orderItems = createTestOrderItems();
        return new OrderResponse(order, orderItems);
    }
    
    private Order createTestOrder() {
        Order order = new Order("ORD-001", "CUST-001");
        order.setStatusEnum(OrderStatus.SHIPPED);
        order.setTotalAmount(new BigDecimal("1500.00"));
        order.setPaymentMethod("CREDIT_CARD");
        order.setShippingAddress("東京都渋谷区渋谷1-1-1, 渋谷区, 東京都 150-0002, JP");
        order.setInventoryReservationId("RES-001");
        order.setPaymentId("PAY-001");
        order.setShipmentId("SHIP-001");
        return order;
    }
    
    private List<OrderItem> createTestOrderItems() {
        OrderItem item = new OrderItem("ORD-001", "PROD-001", "Test Product", 1, new BigDecimal("1500.00"));
        return List.of(item);
    }
}