package com.example.order.controller;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.exception.OrderProcessingException;
import com.example.order.service.OrderProcessService;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.exception.transaction.AbortException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderProcessService orderProcessService;
    
    public OrderController(OrderProcessService orderProcessService) {
        this.orderProcessService = orderProcessService;
    }

    @PostMapping
    @PreAuthorize("#request.customerId == authentication.principal")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request, 
            Authentication authentication) throws OrderProcessingException, ExecutionException, TransactionException, AbortException {
        logger.info("Received order creation request for customer: {} by authenticated user: {}", 
            request.getCustomerId(), authentication.getPrincipal());
        
        OrderResponse response = orderProcessService.createOrder(request);
        logger.info("Order created successfully: {}", response.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId, 
            Authentication authentication) throws OrderProcessingException, ExecutionException, TransactionException, AbortException {
        logger.info("Received get order request for order: {} by authenticated user: {}", 
            orderId, authentication.getPrincipal());
        
        Optional<OrderResponse> response = orderProcessService.getOrder(orderId);
        if (response.isPresent()) {
            OrderResponse order = response.get();
            
            if (!isOrderAccessAllowed(order, authentication)) {
                logger.warn("Customer {} attempted to access order {} belonging to customer {}", 
                    authentication.getPrincipal(), orderId, order.getCustomerId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    @PreAuthorize("#customerId == authentication.principal")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@RequestParam String customerId,
            Authentication authentication) throws OrderProcessingException, ExecutionException, TransactionException, AbortException {
        logger.info("Received get orders request for customer: {} by authenticated user: {}", 
            customerId, authentication.getPrincipal());
        
        List<OrderResponse> orders = orderProcessService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId, Authentication authentication) throws OrderProcessingException, ExecutionException, TransactionException, AbortException {
        logger.info("Received cancel order request for order: {} by authenticated user: {}", 
            orderId, authentication.getPrincipal());
        
        Optional<OrderResponse> orderOpt = orderProcessService.getOrder(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        OrderResponse order = orderOpt.get();
        if (!isOrderAccessAllowed(order, authentication)) {
            logger.warn("Customer {} attempted to cancel order {} belonging to customer {}", 
                authentication.getPrincipal(), orderId, order.getCustomerId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        orderProcessService.cancelOrder(orderId);
        logger.info("Order cancelled successfully: {}", orderId);
        return ResponseEntity.ok().build();
    }

    private boolean isOrderAccessAllowed(OrderResponse order, Authentication authentication) {
        return order.getCustomerId().equals(authentication.getPrincipal());
    }
}