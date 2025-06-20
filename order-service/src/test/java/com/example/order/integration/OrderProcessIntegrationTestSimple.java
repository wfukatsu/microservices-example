package com.example.order.integration;

import com.example.order.OrderServiceApplication;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.entity.OrderStatus;
import com.example.order.service.OrderProcessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Simple E2E integration test without external service dependencies
 */
@SpringBootTest(
    classes = OrderServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class OrderProcessIntegrationTestSimple {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private OrderProcessService orderProcessService;
    
    @Test
    void createOrder_MockedService_ReturnsSuccess() throws Exception {
        // Given
        CreateOrderRequest request = createTestOrderRequest();
        OrderResponse expectedResponse = createMockOrderResponse();
        
        when(orderProcessService.createOrder(any(CreateOrderRequest.class)))
            .thenReturn(expectedResponse);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            entity,
            OrderResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOrderId()).isEqualTo("ORD-12345");
        assertThat(response.getBody().getCustomerId()).isEqualTo("CUST-001");
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.PENDING);
    }
    
    @Test
    void getOrder_MockedService_ReturnsOrder() throws Exception {
        // Given
        String orderId = "ORD-12345";
        OrderResponse expectedResponse = createMockOrderResponse();
        
        when(orderProcessService.getOrder(orderId))
            .thenReturn(java.util.Optional.of(expectedResponse));
        
        // When
        ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/orders/" + orderId,
            OrderResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOrderId()).isEqualTo(orderId);
    }
    
    @Test
    void getOrdersByCustomer_MockedService_ReturnsOrders() throws Exception {
        // Given
        String customerId = "CUST-001";
        List<OrderResponse> expectedResponse = List.of(createMockOrderResponse());
        
        when(orderProcessService.getOrdersByCustomer(customerId))
            .thenReturn(expectedResponse);
        
        // When
        ResponseEntity<OrderResponse[]> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/orders?customerId=" + customerId,
            OrderResponse[].class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getCustomerId()).isEqualTo(customerId);
    }
    
    @Test
    void healthCheck_Application_ReturnsUp() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health",
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
    
    private CreateOrderRequest createTestOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("CUST-001");
        request.setNotes("Test order for E2E testing");
        
        // Items
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId("PROD-001");
        item.setQuantity(2);
        request.setItems(List.of(item));
        
        // Secure payment details (tokenized)
        CreateOrderRequest.PaymentMethodDetails paymentDetails = new CreateOrderRequest.PaymentMethodDetails();
        paymentDetails.setPaymentMethod("CREDIT_CARD");
        paymentDetails.setPaymentToken("tok_test_1234567890");
        paymentDetails.setLast4Digits("4242");
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
    
    private OrderResponse createMockOrderResponse() {
        OrderResponse response = new OrderResponse();
        response.setOrderId("ORD-12345");
        response.setCustomerId("CUST-001");
        response.setStatus(OrderStatus.PENDING);
        response.setTotalAmount(new BigDecimal("3000.00"));
        response.setPaymentMethod("CREDIT_CARD");
        response.setShippingAddress("東京都渋谷区渋谷1-1-1, 渋谷区, 東京都 150-0002, JP");
        response.setCreatedAt(java.time.LocalDateTime.now());
        response.setUpdatedAt(java.time.LocalDateTime.now());
        
        OrderResponse.OrderItemResponse item = new OrderResponse.OrderItemResponse();
        item.setProductId("PROD-001");
        item.setProductName("Test Product");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("1500.00"));
        item.setTotalPrice(new BigDecimal("3000.00"));
        response.setItems(List.of(item));
        
        return response;
    }
}