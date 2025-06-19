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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderProcessService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderProcessService.class);
    
    @Autowired
    private DistributedTransactionManager transactionManager;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private InventoryServiceClient inventoryServiceClient;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Autowired
    private ShippingServiceClient shippingServiceClient;

    @CircuitBreaker(name = "order-process", fallbackMethod = "createOrderFallback")
    @Retry(name = "order-process")
    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderId = generateOrderId();
        logger.info("Starting order creation process for order: {}", orderId);
        
        DistributedTransaction transaction = transactionManager.start();
        
        try {
            // Step 1: Create initial order
            Order order = createInitialOrder(orderId, request);
            orderRepository.save(transaction, order);
            
            // Step 2: Reserve inventory
            logger.info("Reserving inventory for order: {}", orderId);
            InventoryServiceClient.InventoryReservationResponse inventoryReservation = 
                reserveInventory(orderId, request);
            
            order.setInventoryReservationId(inventoryReservation.getReservationId());
            order.setStatusEnum(OrderStatus.INVENTORY_RESERVED);
            orderRepository.save(transaction, order);
            
            // Step 3: Create order items with pricing from inventory
            List<OrderItem> orderItems = createOrderItems(orderId, inventoryReservation);
            orderItemRepository.saveAll(transaction, orderItems);
            
            // Calculate total amount
            BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getTotalPriceDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotalAmountDecimal(totalAmount);
            orderRepository.save(transaction, order);
            
            // Step 4: Process payment
            logger.info("Processing payment for order: {}", orderId);
            PaymentServiceClient.PaymentResponse paymentResponse = 
                processPayment(orderId, request, totalAmount);
            
            order.setPaymentId(paymentResponse.getPaymentId());
            order.setStatusEnum(OrderStatus.PAYMENT_COMPLETED);
            orderRepository.save(transaction, order);
            
            // Step 5: Create shipment
            logger.info("Creating shipment for order: {}", orderId);
            ShippingServiceClient.ShipmentResponse shipmentResponse = 
                createShipment(orderId, request, orderItems);
            
            order.setShipmentId(shipmentResponse.getShipmentId());
            order.setStatusEnum(OrderStatus.SHIPPED);
            orderRepository.save(transaction, order);
            
            // Step 6: Confirm inventory reservation
            logger.info("Confirming inventory reservation for order: {}", orderId);
            inventoryServiceClient.confirmReservation(inventoryReservation.getReservationId());
            
            // Commit transaction
            transaction.commit();
            logger.info("Order creation completed successfully for order: {}", orderId);
            
            return new OrderResponse(order, orderItems);
            
        } catch (Exception e) {
            logger.error("Order creation failed for order: {}, error: {}", orderId, e.getMessage(), e);
            
            try {
                // Compensation logic
                compensateOrder(orderId, transaction);
                transaction.abort();
            } catch (Exception compensationError) {
                logger.error("Compensation failed for order: {}, error: {}", orderId, compensationError.getMessage());
                transaction.abort();
            }
            
            throw new RuntimeException("Failed to create order: " + orderId, e);
        }
    }

    @CircuitBreaker(name = "order-cancel", fallbackMethod = "cancelOrderFallback")
    public void cancelOrder(String orderId) {
        logger.info("Starting order cancellation for order: {}", orderId);
        
        DistributedTransaction transaction = transactionManager.start();
        
        try {
            Optional<Order> orderOpt = orderRepository.findById(transaction, orderId);
            if (orderOpt.isEmpty()) {
                throw new RuntimeException("Order not found: " + orderId);
            }
            
            Order order = orderOpt.get();
            
            if (order.getStatusEnum().isTerminal()) {
                throw new RuntimeException("Cannot cancel order in status: " + order.getStatus());
            }
            
            // Compensation logic
            compensateOrder(orderId, transaction);
            
            // Update order status
            order.setStatusEnum(OrderStatus.CANCELLED);
            orderRepository.save(transaction, order);
            
            transaction.commit();
            logger.info("Order cancelled successfully: {}", orderId);
            
        } catch (Exception e) {
            logger.error("Order cancellation failed for order: {}, error: {}", orderId, e.getMessage(), e);
            transaction.abort();
            throw new RuntimeException("Failed to cancel order: " + orderId, e);
        }
    }

    public Optional<OrderResponse> getOrder(String orderId) {
        DistributedTransaction transaction = transactionManager.start();
        
        try {
            Optional<Order> orderOpt = orderRepository.findById(transaction, orderId);
            if (orderOpt.isEmpty()) {
                transaction.commit();
                return Optional.empty();
            }
            
            Order order = orderOpt.get();
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(transaction, orderId);
            
            transaction.commit();
            return Optional.of(new OrderResponse(order, orderItems));
            
        } catch (Exception e) {
            logger.error("Failed to get order: {}, error: {}", orderId, e.getMessage(), e);
            transaction.abort();
            throw new RuntimeException("Failed to get order: " + orderId, e);
        }
    }

    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        DistributedTransaction transaction = transactionManager.start();
        
        try {
            List<Order> orders = orderRepository.findByCustomerId(transaction, customerId);
            List<OrderResponse> responses = new ArrayList<>();
            
            for (Order order : orders) {
                List<OrderItem> orderItems = orderItemRepository.findByOrderId(transaction, order.getOrderId());
                responses.add(new OrderResponse(order, orderItems));
            }
            
            transaction.commit();
            return responses;
            
        } catch (Exception e) {
            logger.error("Failed to get orders for customer: {}, error: {}", customerId, e.getMessage(), e);
            transaction.abort();
            throw new RuntimeException("Failed to get orders for customer: " + customerId, e);
        }
    }

    private Order createInitialOrder(String orderId, CreateOrderRequest request) {
        Order order = new Order(orderId, request.getCustomerId(), OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethodDetails().getPaymentMethod());
        order.setNotes(request.getNotes());
        
        // Build shipping address from recipient info
        CreateOrderRequest.ShippingInfo.RecipientInfo recipient = request.getShippingInfo().getRecipientInfo();
        String shippingAddress = String.format("%s, %s, %s %s, %s", 
            recipient.getAddress(), recipient.getCity(), recipient.getState(), 
            recipient.getPostalCode(), recipient.getCountry());
        order.setShippingAddress(shippingAddress);
        
        return order;
    }

    private InventoryServiceClient.InventoryReservationResponse reserveInventory(String orderId, CreateOrderRequest request) {
        InventoryServiceClient.ReserveInventoryRequest inventoryRequest = 
            new InventoryServiceClient.ReserveInventoryRequest();
        inventoryRequest.setOrderId(orderId);
        inventoryRequest.setCustomerId(request.getCustomerId());
        
        List<InventoryServiceClient.InventoryItemRequest> inventoryItems = request.getItems().stream()
            .map(item -> new InventoryServiceClient.InventoryItemRequest(item.getProductId(), item.getQuantity()))
            .collect(Collectors.toList());
        inventoryRequest.setItems(inventoryItems);
        
        return inventoryServiceClient.reserveInventory(inventoryRequest);
    }

    private List<OrderItem> createOrderItems(String orderId, InventoryServiceClient.InventoryReservationResponse reservation) {
        return reservation.getItems().stream()
            .map(reservedItem -> new OrderItem(
                orderId,
                reservedItem.getProductId(),
                reservedItem.getProductName(),
                reservedItem.getReservedQuantity(),
                reservedItem.getUnitPrice()
            ))
            .collect(Collectors.toList());
    }

    private PaymentServiceClient.PaymentResponse processPayment(String orderId, CreateOrderRequest request, BigDecimal totalAmount) {
        PaymentServiceClient.ProcessPaymentRequest paymentRequest = 
            new PaymentServiceClient.ProcessPaymentRequest();
        paymentRequest.setOrderId(orderId);
        paymentRequest.setCustomerId(request.getCustomerId());
        paymentRequest.setAmount(totalAmount);
        paymentRequest.setCurrency("JPY");
        paymentRequest.setPaymentMethod(request.getPaymentMethodDetails().getPaymentMethod());
        paymentRequest.setDescription("Order payment for order: " + orderId);
        
        // Convert payment method details
        PaymentServiceClient.PaymentMethodDetails paymentMethodDetails = 
            new PaymentServiceClient.PaymentMethodDetails();
        CreateOrderRequest.PaymentMethodDetails srcDetails = request.getPaymentMethodDetails();
        paymentMethodDetails.setCardNumber(srcDetails.getCardNumber());
        paymentMethodDetails.setExpiryMonth(srcDetails.getExpiryMonth());
        paymentMethodDetails.setExpiryYear(srcDetails.getExpiryYear());
        paymentMethodDetails.setCvv(srcDetails.getCvv());
        paymentMethodDetails.setCardholderName(srcDetails.getCardholderName());
        paymentRequest.setPaymentMethodDetails(paymentMethodDetails);
        
        return paymentServiceClient.processPayment(paymentRequest);
    }

    private ShippingServiceClient.ShipmentResponse createShipment(String orderId, CreateOrderRequest request, List<OrderItem> orderItems) {
        ShippingServiceClient.CreateShipmentRequest shipmentRequest = 
            new ShippingServiceClient.CreateShipmentRequest();
        shipmentRequest.setOrderId(orderId);
        shipmentRequest.setCustomerId(request.getCustomerId());
        shipmentRequest.setShippingMethod(request.getShippingInfo().getShippingMethod());
        shipmentRequest.setCarrier(request.getShippingInfo().getCarrier());
        
        // Convert recipient info
        ShippingServiceClient.CreateShipmentRequest.RecipientInfo recipientInfo = 
            new ShippingServiceClient.CreateShipmentRequest.RecipientInfo();
        CreateOrderRequest.ShippingInfo.RecipientInfo srcRecipient = request.getShippingInfo().getRecipientInfo();
        recipientInfo.setName(srcRecipient.getName());
        recipientInfo.setPhone(srcRecipient.getPhone());
        recipientInfo.setAddress(srcRecipient.getAddress());
        recipientInfo.setCity(srcRecipient.getCity());
        recipientInfo.setState(srcRecipient.getState());
        recipientInfo.setPostalCode(srcRecipient.getPostalCode());
        recipientInfo.setCountry(srcRecipient.getCountry());
        shipmentRequest.setRecipientInfo(recipientInfo);
        
        // Convert package info
        if (request.getShippingInfo().getPackageInfo() != null) {
            ShippingServiceClient.CreateShipmentRequest.PackageInfo packageInfo = 
                new ShippingServiceClient.CreateShipmentRequest.PackageInfo();
            CreateOrderRequest.ShippingInfo.PackageInfo srcPackage = request.getShippingInfo().getPackageInfo();
            packageInfo.setWeight(srcPackage.getWeight());
            packageInfo.setDimensions(srcPackage.getDimensions());
            packageInfo.setSpecialInstructions(srcPackage.getSpecialInstructions());
            shipmentRequest.setPackageInfo(packageInfo);
        }
        
        // Convert shipping items
        List<ShippingServiceClient.CreateShipmentRequest.ShippingItemRequest> shippingItems = 
            orderItems.stream()
                .map(orderItem -> {
                    ShippingServiceClient.CreateShipmentRequest.ShippingItemRequest shippingItem = 
                        new ShippingServiceClient.CreateShipmentRequest.ShippingItemRequest();
                    shippingItem.setProductId(orderItem.getProductId());
                    shippingItem.setProductName(orderItem.getProductName());
                    shippingItem.setQuantity(orderItem.getQuantity());
                    shippingItem.setWeight(orderItem.getWeightDecimal().doubleValue());
                    return shippingItem;
                })
                .collect(Collectors.toList());
        shipmentRequest.setItems(shippingItems);
        
        return shippingServiceClient.createShipment(shipmentRequest);
    }

    private void compensateOrder(String orderId, DistributedTransaction transaction) {
        logger.info("Starting compensation for order: {}", orderId);
        
        try {
            Optional<Order> orderOpt = orderRepository.findById(transaction, orderId);
            if (orderOpt.isEmpty()) {
                return;
            }
            
            Order order = orderOpt.get();
            
            // Cancel shipment if created
            if (order.getShipmentId() != null) {
                try {
                    shippingServiceClient.cancelShipment(order.getShipmentId());
                    logger.info("Cancelled shipment: {} for order: {}", order.getShipmentId(), orderId);
                } catch (Exception e) {
                    logger.warn("Failed to cancel shipment: {} for order: {}, error: {}", 
                        order.getShipmentId(), orderId, e.getMessage());
                }
            }
            
            // Refund payment if processed
            if (order.getPaymentId() != null) {
                try {
                    PaymentServiceClient.RefundRequest refundRequest = 
                        new PaymentServiceClient.RefundRequest(order.getTotalAmountDecimal(), "Order cancellation");
                    paymentServiceClient.refundPayment(order.getPaymentId(), refundRequest);
                    logger.info("Refunded payment: {} for order: {}", order.getPaymentId(), orderId);
                } catch (Exception e) {
                    logger.warn("Failed to refund payment: {} for order: {}, error: {}", 
                        order.getPaymentId(), orderId, e.getMessage());
                }
            }
            
            // Cancel inventory reservation if created
            if (order.getInventoryReservationId() != null) {
                try {
                    inventoryServiceClient.cancelReservation(order.getInventoryReservationId());
                    logger.info("Cancelled inventory reservation: {} for order: {}", 
                        order.getInventoryReservationId(), orderId);
                } catch (Exception e) {
                    logger.warn("Failed to cancel inventory reservation: {} for order: {}, error: {}", 
                        order.getInventoryReservationId(), orderId, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logger.error("Compensation process failed for order: {}, error: {}", orderId, e.getMessage(), e);
        }
    }

    private String generateOrderId() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Fallback methods for circuit breaker
    public OrderResponse createOrderFallback(CreateOrderRequest request, Exception ex) {
        logger.error("Circuit breaker activated for order creation, error: {}", ex.getMessage());
        throw new RuntimeException("Order service is temporarily unavailable. Please try again later.");
    }

    public void cancelOrderFallback(String orderId, Exception ex) {
        logger.error("Circuit breaker activated for order cancellation: {}, error: {}", orderId, ex.getMessage());
        throw new RuntimeException("Order cancellation service is temporarily unavailable. Please try again later.");
    }
}