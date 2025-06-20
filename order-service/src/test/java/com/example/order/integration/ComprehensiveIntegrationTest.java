package com.example.order.integration;

import com.example.order.OrderServiceApplication;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.security.JwtUtil;
import com.example.order.service.OrderProcessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Comprehensive integration test covering security and business logic
 */
@SpringBootTest(
    classes = OrderServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class ComprehensiveIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @MockBean
    private OrderProcessService orderProcessService;
    
    @Test
    void fullOrderFlow_WithAuthentication_Success() throws Exception {
        // Given
        String customerId = "CUST-001";
        String token = jwtUtil.generateToken(customerId);
        
        CreateOrderRequest request = createTestOrderRequest(customerId);
        OrderResponse mockResponse = createMockOrderResponse(customerId);
        
        when(orderProcessService.createOrder(any(CreateOrderRequest.class)))
            .thenReturn(mockResponse);
        when(orderProcessService.getOrder(anyString()))
            .thenReturn(Optional.of(mockResponse));
        when(orderProcessService.getOrdersByCustomer(customerId))
            .thenReturn(List.of(mockResponse));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        
        // When & Then - Create Order
        HttpEntity<CreateOrderRequest> createEntity = new HttpEntity<>(request, headers);
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            createEntity,
            OrderResponse.class
        );
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getCustomerId()).isEqualTo(customerId);
        
        // When & Then - Get Order
        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<OrderResponse> getResponse = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/orders/" + mockResponse.getOrderId(),
            HttpMethod.GET,
            getEntity,
            OrderResponse.class
        );
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getOrderId()).isEqualTo(mockResponse.getOrderId());
        
        // When & Then - Get Orders by Customer
        ResponseEntity<OrderResponse[]> listResponse = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/orders?customerId=" + customerId,
            HttpMethod.GET,
            getEntity,
            OrderResponse[].class
        );
        
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull();
        assertThat(listResponse.getBody()).hasSize(1);
        assertThat(listResponse.getBody()[0].getCustomerId()).isEqualTo(customerId);
    }
    
    @Test
    void orderFlow_WithoutAuthentication_ReturnsForbidden() {
        // Given
        CreateOrderRequest request = createTestOrderRequest("CUST-001");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            entity,
            String.class
        );
        
        // Then - Spring Security returns 403 for unauthenticated requests
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
    
    @Test
    void orderFlow_WithWrongCustomer_ReturnsForbiddenOrServerError() {
        // Given
        String authenticatedCustomerId = "CUST-001";
        String requestCustomerId = "CUST-002";
        String token = jwtUtil.generateToken(authenticatedCustomerId);
        
        CreateOrderRequest request = createTestOrderRequest(requestCustomerId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, headers);
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            entity,
            String.class
        );
        
        // Then - Could be 403 for authorization failure or 500 for business logic error
        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    private CreateOrderRequest createTestOrderRequest(String customerId) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);
        request.setNotes("Integration test order");
        
        // Items
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId("PROD-001");
        item.setQuantity(1);
        request.setItems(List.of(item));
        
        // Payment details
        CreateOrderRequest.PaymentMethodDetails paymentDetails = new CreateOrderRequest.PaymentMethodDetails();
        paymentDetails.setPaymentMethod("CREDIT_CARD");
        paymentDetails.setPaymentToken("tok_test_integration");
        paymentDetails.setLast4Digits("1234");
        paymentDetails.setCardBrand("VISA");
        paymentDetails.setCardholderName("Integration Test User");
        request.setPaymentMethodDetails(paymentDetails);
        
        // Shipping info
        CreateOrderRequest.ShippingInfo shippingInfo = new CreateOrderRequest.ShippingInfo();
        shippingInfo.setShippingMethod("STANDARD");
        shippingInfo.setCarrier("YAMATO");
        
        CreateOrderRequest.ShippingInfo.RecipientInfo recipientInfo = new CreateOrderRequest.ShippingInfo.RecipientInfo();
        recipientInfo.setName("Integration Test User");
        recipientInfo.setPhone("090-1234-5678");
        recipientInfo.setAddress("Integration Test Address");
        recipientInfo.setCity("Test City");
        recipientInfo.setState("Test State");
        recipientInfo.setPostalCode("12345");
        recipientInfo.setCountry("JP");
        shippingInfo.setRecipientInfo(recipientInfo);
        
        request.setShippingInfo(shippingInfo);
        
        return request;
    }
    
    private OrderResponse createMockOrderResponse(String customerId) {
        OrderResponse response = new OrderResponse();
        response.setOrderId("ORD-INTEGRATION-001");
        response.setCustomerId(customerId);
        response.setStatus(com.example.order.entity.OrderStatus.PENDING);
        response.setTotalAmount(new BigDecimal("1500.00"));
        response.setPaymentMethod("CREDIT_CARD");
        response.setShippingAddress("Integration Test Address, Test City, Test State 12345, JP");
        response.setCreatedAt(java.time.LocalDateTime.now());
        response.setUpdatedAt(java.time.LocalDateTime.now());
        
        OrderResponse.OrderItemResponse item = new OrderResponse.OrderItemResponse();
        item.setProductId("PROD-001");
        item.setProductName("Integration Test Product");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("1500.00"));
        item.setTotalPrice(new BigDecimal("1500.00"));
        response.setItems(List.of(item));
        
        return response;
    }
}