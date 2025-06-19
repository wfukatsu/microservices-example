package com.example.order.integration;

import com.example.order.OrderServiceApplication;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(
    classes = OrderServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class OrderProcessIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private WireMockServer inventoryService;
    private WireMockServer paymentService;
    private WireMockServer shippingService;
    
    @BeforeEach
    void setUp() {
        // Setup WireMock servers for external services
        inventoryService = new WireMockServer(8081);
        paymentService = new WireMockServer(8082);
        shippingService = new WireMockServer(8083);
        
        inventoryService.start();
        paymentService.start();
        shippingService.start();
        
        setupMockResponses();
    }
    
    @AfterEach
    void tearDown() {
        inventoryService.stop();
        paymentService.stop();
        shippingService.stop();
    }
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("services.inventory.url", () -> "http://localhost:8081");
        registry.add("services.payment.url", () -> "http://localhost:8082");
        registry.add("services.shipping.url", () -> "http://localhost:8083");
    }
    
    @Test
    void createOrder_SuccessfulFlow_ReturnsCompletedOrder() throws Exception {
        // Given
        CreateOrderRequest request = createTestOrderRequest();
        
        // When
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            request,
            OrderResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        
        OrderResponse orderResponse = response.getBody();
        assertThat(orderResponse.getCustomerId()).isEqualTo("CUST-001");
        assertThat(orderResponse.getStatus().toString()).isEqualTo("SHIPPED");
        assertThat(orderResponse.getTotalAmount()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(orderResponse.getInventoryReservationId()).isEqualTo("RES-12345");
        assertThat(orderResponse.getPaymentId()).isEqualTo("PAY-67890");
        assertThat(orderResponse.getShipmentId()).isEqualTo("SHIP-11111");
        assertThat(orderResponse.getItems()).hasSize(1);
        
        // Verify external service calls
        inventoryService.verify(postRequestedFor(urlEqualTo("/api/v1/inventory/reserve")));
        inventoryService.verify(postRequestedFor(urlMatching("/api/v1/inventory/confirm/.*")));
        paymentService.verify(postRequestedFor(urlEqualTo("/api/v1/payments/process")));
        shippingService.verify(postRequestedFor(urlEqualTo("/api/v1/shipping/shipments")));
    }
    
    @Test
    void createOrder_InventoryFailure_ReturnsError() throws Exception {
        // Given
        CreateOrderRequest request = createTestOrderRequest();
        
        // Override inventory service to return error
        inventoryService.stubFor(post(urlEqualTo("/api/v1/inventory/reserve"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\": \"Inventory service unavailable\"}")));
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            request,
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        
        // Verify no downstream calls were made
        paymentService.verify(0, postRequestedFor(urlEqualTo("/api/v1/payments/process")));
        shippingService.verify(0, postRequestedFor(urlEqualTo("/api/v1/shipping/shipments")));
    }
    
    @Test
    void createOrder_PaymentFailure_ExecutesCompensation() throws Exception {
        // Given
        CreateOrderRequest request = createTestOrderRequest();
        
        // Override payment service to return error
        paymentService.stubFor(post(urlEqualTo("/api/v1/payments/process"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\": \"Payment declined\"}")));
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            request,
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        
        // Verify compensation calls
        inventoryService.verify(postRequestedFor(urlEqualTo("/api/v1/inventory/reserve")));
        inventoryService.verify(postRequestedFor(urlMatching("/api/v1/inventory/cancel/.*")));
        shippingService.verify(0, postRequestedFor(urlEqualTo("/api/v1/shipping/shipments")));
    }
    
    @Test
    void getOrder_ExistingOrder_ReturnsOrderDetails() throws Exception {
        // Given - First create an order
        CreateOrderRequest createRequest = createTestOrderRequest();
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            createRequest,
            OrderResponse.class
        );
        
        String orderId = createResponse.getBody().getOrderId();
        
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
    void getOrder_NonExistentOrder_ReturnsNotFound() throws Exception {
        // When
        ResponseEntity<OrderResponse> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/orders/NON-EXISTENT",
            OrderResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    
    @Test
    void getOrdersByCustomer_ReturnsCustomerOrders() throws Exception {
        // Given - Create two orders for the same customer
        CreateOrderRequest request1 = createTestOrderRequest();
        CreateOrderRequest request2 = createTestOrderRequest();
        request2.setNotes("Second order");
        
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            request1,
            OrderResponse.class
        );
        
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            request2,
            OrderResponse.class
        );
        
        // When
        ResponseEntity<OrderResponse[]> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/orders?customerId=CUST-001",
            OrderResponse[].class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(2);
    }
    
    @Test
    void cancelOrder_ValidOrder_SuccessfullyCancels() throws Exception {
        // Given - Create an order first
        CreateOrderRequest createRequest = createTestOrderRequest();
        ResponseEntity<OrderResponse> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            createRequest,
            OrderResponse.class
        );
        
        String orderId = createResponse.getBody().getOrderId();
        
        // Setup compensation service responses
        setupCompensationMocks();
        
        // When
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders/" + orderId + "/cancel",
            null,
            Void.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify compensation calls were made
        inventoryService.verify(postRequestedFor(urlMatching("/api/v1/inventory/cancel/.*")));
        paymentService.verify(postRequestedFor(urlMatching("/api/v1/payments/.*/refund")));
        shippingService.verify(postRequestedFor(urlMatching("/api/v1/shipping/shipments/.*/cancel")));
    }
    
    private void setupMockResponses() {
        // Mock inventory service responses
        inventoryService.stubFor(post(urlEqualTo("/api/v1/inventory/reserve"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(createInventoryReservationResponse())));
        
        inventoryService.stubFor(post(urlMatching("/api/v1/inventory/confirm/.*"))
            .willReturn(aResponse()
                .withStatus(200)));
        
        // Mock payment service responses
        paymentService.stubFor(post(urlEqualTo("/api/v1/payments/process"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(createPaymentResponse())));
        
        // Mock shipping service responses
        shippingService.stubFor(post(urlEqualTo("/api/v1/shipping/shipments"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(createShipmentResponse())));
    }
    
    private void setupCompensationMocks() {
        // Mock compensation service responses
        inventoryService.stubFor(post(urlMatching("/api/v1/inventory/cancel/.*"))
            .willReturn(aResponse().withStatus(200)));
        
        paymentService.stubFor(post(urlMatching("/api/v1/payments/.*/refund"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(createRefundResponse())));
        
        shippingService.stubFor(post(urlMatching("/api/v1/shipping/shipments/.*/cancel"))
            .willReturn(aResponse().withStatus(200)));
    }
    
    private CreateOrderRequest createTestOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("CUST-001");
        request.setNotes("Integration test order");
        
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
    
    private String createInventoryReservationResponse() {
        return """
            {
                "reservationId": "RES-12345",
                "orderId": "ORD-001",
                "customerId": "CUST-001",
                "status": "RESERVED",
                "expiresAt": "%s",
                "createdAt": "%s",
                "items": [
                    {
                        "productId": "PROD-001",
                        "productName": "Test Product",
                        "reservedQuantity": 1,
                        "unitPrice": 1500.00
                    }
                ]
            }
            """.formatted(
                LocalDateTime.now().plusHours(24).toString(),
                LocalDateTime.now().toString()
            );
    }
    
    private String createPaymentResponse() {
        return """
            {
                "paymentId": "PAY-67890",
                "orderId": "ORD-001",
                "customerId": "CUST-001",
                "amount": 1500.00,
                "currency": "JPY",
                "status": "COMPLETED",
                "paymentMethod": "CREDIT_CARD",
                "transactionId": "TXN-001",
                "authorizationCode": "AUTH-001",
                "processedAt": "%s"
            }
            """.formatted(LocalDateTime.now().toString());
    }
    
    private String createShipmentResponse() {
        return """
            {
                "shipmentId": "SHIP-11111",
                "orderId": "ORD-001",
                "customerId": "CUST-001",
                "status": "PROCESSING",
                "carrier": "YAMATO",
                "trackingNumber": "ST123456789012",
                "shippingMethod": "STANDARD",
                "estimatedDeliveryDate": "%s",
                "createdAt": "%s"
            }
            """.formatted(
                LocalDateTime.now().plusDays(3).toString(),
                LocalDateTime.now().toString()
            );
    }
    
    private String createRefundResponse() {
        return """
            {
                "refundId": "REF-12345",
                "paymentId": "PAY-67890",
                "refundAmount": 1500.00,
                "status": "COMPLETED",
                "processedAt": "%s"
            }
            """.formatted(LocalDateTime.now().toString());
    }
}