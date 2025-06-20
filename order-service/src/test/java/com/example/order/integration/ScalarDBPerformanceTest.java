package com.example.order.integration;

import com.example.order.entity.Order;
import com.example.order.entity.OrderItem;
import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.OrderItemRepository;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance tests for ScalarDB operations
 * 
 * Run with: mvn test -Dtest.performance=true -Dtest=ScalarDBPerformanceTest
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "test.performance", matches = "true")
class ScalarDBPerformanceTest {

    @Autowired
    private DistributedTransactionManager transactionManager;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private static final int CONCURRENT_TRANSACTIONS = 10;
    private static final int ORDERS_PER_TRANSACTION = 5;

    @BeforeEach
    void setUp() {
        // Performance tests setup
    }

    @Test
    void batchOrderCreation_MultipleOrders_CompletesWithinTimeLimit() throws Exception {
        // Given
        List<Order> orders = createTestOrders(50);
        long startTime = System.currentTimeMillis();

        // When
        DistributedTransaction transaction = transactionManager.start();
        try {
            for (Order order : orders) {
                orderRepository.create(order, transaction);
            }
            transaction.commit();
        } catch (Exception e) {
            transaction.abort();
            throw e;
        }

        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration).isLessThan(5000); // Should complete within 5 seconds
        
        // Verify all orders were created
        DistributedTransaction verificationTransaction = transactionManager.start();
        for (Order order : orders) {
            var foundOrder = orderRepository.findById(order.getOrderId(), verificationTransaction);
            assertThat(foundOrder).isPresent();
        }
        verificationTransaction.commit();
    }

    @Test
    void concurrentOrderCreation_MultipleThreads_AllSucceed() throws Exception {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_TRANSACTIONS);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // When
        for (int i = 0; i < CONCURRENT_TRANSACTIONS; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    List<Order> orders = createTestOrders(ORDERS_PER_TRANSACTION, "THREAD-" + threadId);
                    
                    DistributedTransaction transaction = transactionManager.start();
                    try {
                        for (Order order : orders) {
                            orderRepository.create(order, transaction);
                        }
                        transaction.commit();
                    } catch (Exception e) {
                        transaction.abort();
                        throw new RuntimeException(e);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
            
            futures.add(future);
        }

        // Wait for all threads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

        // Then
        executor.shutdown();
        
        // Verify total orders created
        DistributedTransaction verificationTransaction = transactionManager.start();
        for (int i = 0; i < CONCURRENT_TRANSACTIONS; i++) {
            List<Order> customerOrders = orderRepository.findByCustomerId("CUST-THREAD-" + i, verificationTransaction);
            assertThat(customerOrders).hasSize(ORDERS_PER_TRANSACTION);
        }
        verificationTransaction.commit();
    }

    @Test
    void batchOrderItemRetrieval_LargeDataset_PerformsEfficiently() throws Exception {
        // Given - Create orders with multiple items each
        List<Order> orders = createTestOrders(20);
        List<OrderItem> allOrderItems = new ArrayList<>();
        
        DistributedTransaction setupTransaction = transactionManager.start();
        try {
            for (Order order : orders) {
                orderRepository.create(order, setupTransaction);
                
                // Create 5 items per order
                for (int i = 0; i < 5; i++) {
                    OrderItem item = createTestOrderItem(order.getOrderId(), i);
                    orderItemRepository.create(item, setupTransaction);
                    allOrderItems.add(item);
                }
            }
            setupTransaction.commit();
        } catch (Exception e) {
            setupTransaction.abort();
            throw e;
        }

        // When
        long startTime = System.currentTimeMillis();
        DistributedTransaction transaction = transactionManager.start();
        
        List<String> orderIds = orders.stream().map(Order::getOrderId).toList();
        var orderItemsMap = orderItemRepository.findByOrderIds(orderIds, transaction);
        
        transaction.commit();
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration).isLessThan(2000); // Should complete within 2 seconds
        assertThat(orderItemsMap).hasSize(orders.size());
        
        // Verify all items were retrieved
        int totalItemsRetrieved = orderItemsMap.values().stream()
                .mapToInt(List::size)
                .sum();
        assertThat(totalItemsRetrieved).isEqualTo(allOrderItems.size());
    }

    @Test
    void customerOrderLookup_SecondaryIndex_PerformsEfficiently() throws Exception {
        // Given - Create many orders for one customer
        String customerId = "CUST-PERFORMANCE-TEST";
        List<Order> orders = createTestOrdersForCustomer(100, customerId);
        
        DistributedTransaction setupTransaction = transactionManager.start();
        try {
            for (Order order : orders) {
                orderRepository.create(order, setupTransaction);
            }
            setupTransaction.commit();
        } catch (Exception e) {
            setupTransaction.abort();
            throw e;
        }

        // When
        long startTime = System.currentTimeMillis();
        DistributedTransaction transaction = transactionManager.start();
        
        List<Order> customerOrders = orderRepository.findByCustomerId(customerId, transaction);
        
        transaction.commit();
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(duration).isLessThan(1000); // Should complete within 1 second
        assertThat(customerOrders).hasSize(orders.size());
    }

    private List<Order> createTestOrders(int count) {
        return createTestOrders(count, "TEST");
    }

    private List<Order> createTestOrders(int count, String prefix) {
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Order order = new Order();
            order.setOrderId("ORD-PERF-" + prefix + "-" + i + "-" + UUID.randomUUID().toString().substring(0, 8));
            order.setCustomerId("CUST-" + prefix + "-" + (i % 10)); // Distribute across 10 customers
            order.setStatusEnum(OrderStatus.PENDING);
            order.setTotalAmount(new BigDecimal("1000.00").add(new BigDecimal(i * 100)));
            order.setCurrency("JPY");
            order.setPaymentMethod("CREDIT_CARD");
            order.setShippingAddress("Performance Test Address " + i);
            order.setNotes("Performance test order " + i);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orders.add(order);
        }
        return orders;
    }

    private List<Order> createTestOrdersForCustomer(int count, String customerId) {
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Order order = new Order();
            order.setOrderId("ORD-CUST-PERF-" + i + "-" + UUID.randomUUID().toString().substring(0, 8));
            order.setCustomerId(customerId);
            order.setStatusEnum(OrderStatus.PENDING);
            order.setTotalAmount(new BigDecimal("1500.00").add(new BigDecimal(i * 50)));
            order.setCurrency("JPY");
            order.setPaymentMethod("CREDIT_CARD");
            order.setShippingAddress("Customer Performance Test Address " + i);
            order.setNotes("Customer performance test order " + i);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orders.add(order);
        }
        return orders;
    }

    private OrderItem createTestOrderItem(String orderId, int index) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId("PROD-PERF-" + index);
        item.setProductName("Performance Test Product " + index);
        item.setQuantity(1 + index);
        item.setUnitPrice(new BigDecimal("500.00"));
        item.setTotalPrice(new BigDecimal("500.00").multiply(new BigDecimal(1 + index)));
        item.setSku("PERF-SKU-" + index);
        item.setNotes("Performance test item " + index);
        item.setCreatedAt(LocalDateTime.now());
        return item;
    }
}