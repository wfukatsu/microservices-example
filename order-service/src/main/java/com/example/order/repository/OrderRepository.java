package com.example.order.repository;

import com.example.order.entity.Order;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextColumn;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {
    
    private static final String NAMESPACE = "order_service";
    private static final String TABLE_NAME = "orders";

    public void save(DistributedTransaction transaction, Order order) {
        Put put = Put.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(order.getPartitionKey())
            .build();
        
        order.getColumns().forEach((name, column) -> put.withValue(column));
        
        try {
            transaction.put(put);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save order: " + order.getOrderId(), e);
        }
    }

    public Optional<Order> findById(DistributedTransaction transaction, String orderId) {
        Get get = Get.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .partitionKey(Key.ofText("order_id", orderId))
            .build();
        
        try {
            Optional<Result> result = transaction.get(get);
            return result.map(this::mapToOrder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find order: " + orderId, e);
        }
    }

    public List<Order> findByCustomerId(DistributedTransaction transaction, String customerId) {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .all()
            .build();
        
        List<Order> orders = new ArrayList<>();
        try {
            List<Result> results = transaction.scan(scan);
            for (Result result : results) {
                Order order = mapToOrder(result);
                if (customerId.equals(order.getCustomerId())) {
                    orders.add(order);
                }
            }
            return orders;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find orders for customer: " + customerId, e);
        }
    }

    public List<Order> findByStatus(DistributedTransaction transaction, String status) {
        Scan scan = Scan.newBuilder()
            .namespace(NAMESPACE)
            .table(TABLE_NAME)
            .all()
            .build();
        
        List<Order> orders = new ArrayList<>();
        try {
            List<Result> results = transaction.scan(scan);
            for (Result result : results) {
                Order order = mapToOrder(result);
                if (status.equals(order.getStatus())) {
                    orders.add(order);
                }
            }
            return orders;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find orders by status: " + status, e);
        }
    }

    private Order mapToOrder(Result result) {
        Order order = new Order();
        
        result.getValue("order_id").ifPresent(value -> 
            order.setOrderId(value.getAsString().orElse(null)));
        result.getValue("customer_id").ifPresent(value -> 
            order.setCustomerId(value.getAsString().orElse(null)));
        result.getValue("status").ifPresent(value -> 
            order.setStatus(value.getAsString().orElse(null)));
        result.getValue("total_amount").ifPresent(value -> 
            order.setTotalAmount(value.getAsLong().orElse(0L)));
        result.getValue("currency").ifPresent(value -> 
            order.setCurrency(value.getAsString().orElse(null)));
        result.getValue("payment_method").ifPresent(value -> 
            order.setPaymentMethod(value.getAsString().orElse(null)));
        result.getValue("shipping_address").ifPresent(value -> 
            order.setShippingAddress(value.getAsString().orElse(null)));
        result.getValue("notes").ifPresent(value -> 
            order.setNotes(value.getAsString().orElse(null)));
        result.getValue("created_at").ifPresent(value -> 
            order.setCreatedAt(value.getAsLong().orElse(0L)));
        result.getValue("updated_at").ifPresent(value -> 
            order.setUpdatedAt(value.getAsLong().orElse(0L)));
        result.getValue("inventory_reservation_id").ifPresent(value -> 
            order.setInventoryReservationId(value.getAsString().orElse(null)));
        result.getValue("payment_id").ifPresent(value -> 
            order.setPaymentId(value.getAsString().orElse(null)));
        result.getValue("shipment_id").ifPresent(value -> 
            order.setShipmentId(value.getAsString().orElse(null)));
        
        return order;
    }
}