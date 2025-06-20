package com.example.order.repository;

import com.example.order.entity.Order;
import com.scalar.db.api.*;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.exception.transaction.AbortException;
import com.scalar.db.exception.transaction.CrudConflictException;
import com.scalar.db.exception.transaction.CrudException;
import com.scalar.db.io.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    private static final String NAMESPACE = "order_service";
    private static final String TABLE_NAME = "orders";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DistributedTransactionManager transactionManager;
    
    public OrderRepository(DistributedTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public Order create(Order order, DistributedTransaction transaction) throws ExecutionException, CrudConflictException, CrudException {
        // Insert into main orders table
        Put put = Put.newBuilder()
                .namespace(NAMESPACE)
                .table(TABLE_NAME)
                .partitionKey(Key.ofText("order_id", order.getOrderId()))
                .textValue("customer_id", order.getCustomerId())
                .textValue("status", order.getStatus())
                .textValue("currency", order.getCurrency())
                .textValue("total_amount", order.getTotalAmount() != null ? order.getTotalAmount().toString() : "0")
                .textValue("payment_method", order.getPaymentMethod() != null ? order.getPaymentMethod() : "")
                .textValue("shipping_address", order.getShippingAddress() != null ? order.getShippingAddress() : "")
                .textValue("notes", order.getNotes() != null ? order.getNotes() : "")
                .textValue("inventory_reservation_id", order.getInventoryReservationId() != null ? order.getInventoryReservationId() : "")
                .textValue("payment_id", order.getPaymentId() != null ? order.getPaymentId() : "")
                .textValue("shipment_id", order.getShipmentId() != null ? order.getShipmentId() : "")
                .textValue("created_at", String.valueOf(order.getCreatedAt().toEpochSecond(ZoneOffset.UTC)))
                .textValue("updated_at", String.valueOf(order.getUpdatedAt().toEpochSecond(ZoneOffset.UTC)))
                .build();
        transaction.put(put);
        
        // Maintain secondary index for customer queries
        createCustomerOrderIndex(order, transaction);
        
        return order;
    }

    public Optional<Order> findById(String orderId, DistributedTransaction transaction) throws ExecutionException, CrudConflictException, CrudException {
        Get get = Get.newBuilder()
                .namespace(NAMESPACE)
                .table(TABLE_NAME)
                .partitionKey(Key.ofText("order_id", orderId))
                .build();

        Optional<Result> result = transaction.get(get);
        if (result.isPresent()) {
            return Optional.of(mapResultToEntity(result.get()));
        }
        return Optional.empty();
    }

    public List<Order> findByCustomerId(String customerId, DistributedTransaction transaction) throws ExecutionException, CrudConflictException, CrudException {
        // Use efficient secondary index table for customer queries
        Scan scan = Scan.newBuilder()
                .namespace(NAMESPACE)
                .table("orders_by_customer")
                .partitionKey(Key.ofText("customer_id", customerId))
                .build();

        List<Result> customerOrderResults = transaction.scan(scan);
        List<Order> orders = new ArrayList<>();
        
        // Fetch full order details for each order_id found in the index
        for (Result result : customerOrderResults) {
            String orderId = result.getValue("order_id").get().getAsString().get();
            Optional<Order> orderOpt = findById(orderId, transaction);
            if (orderOpt.isPresent()) {
                orders.add(orderOpt.get());
            }
        }
        
        return orders;
    }

    public Order update(Order order, DistributedTransaction transaction) throws ExecutionException, CrudConflictException, CrudException {
        Get get = Get.newBuilder()
                .namespace(NAMESPACE)
                .table(TABLE_NAME)
                .partitionKey(Key.ofText("order_id", order.getOrderId()))
                .build();

        Optional<Result> existing = transaction.get(get);
        if (!existing.isPresent()) {
            throw new ExecutionException("Order not found for update");
        }

        Put put = Put.newBuilder()
                .namespace(NAMESPACE)
                .table(TABLE_NAME)
                .partitionKey(Key.ofText("order_id", order.getOrderId()))
                .textValue("customer_id", order.getCustomerId())
                .textValue("status", order.getStatus())
                .textValue("currency", order.getCurrency())
                .textValue("total_amount", order.getTotalAmount() != null ? order.getTotalAmount().toString() : "0")
                .textValue("payment_method", order.getPaymentMethod() != null ? order.getPaymentMethod() : "")
                .textValue("shipping_address", order.getShippingAddress() != null ? order.getShippingAddress() : "")
                .textValue("notes", order.getNotes() != null ? order.getNotes() : "")
                .textValue("inventory_reservation_id", order.getInventoryReservationId() != null ? order.getInventoryReservationId() : "")
                .textValue("payment_id", order.getPaymentId() != null ? order.getPaymentId() : "")
                .textValue("shipment_id", order.getShipmentId() != null ? order.getShipmentId() : "")
                .textValue("created_at", existing.get().getValue("created_at").get().getAsString().get())
                .textValue("updated_at", String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)))
                .build();
        transaction.put(put);
        return order;
    }

    public void deleteById(String orderId, DistributedTransaction transaction) throws ExecutionException, CrudConflictException, CrudException {
        // First get the order to obtain customer_id for index deletion
        Optional<Order> orderOpt = findById(orderId, transaction);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            
            // Delete from secondary index first
            deleteCustomerOrderIndex(order.getCustomerId(), orderId, transaction);
            
            // Delete from main table
            Delete delete = Delete.newBuilder()
                    .namespace(NAMESPACE)
                    .table(TABLE_NAME)
                    .partitionKey(Key.ofText("order_id", orderId))
                    .build();
            transaction.delete(delete);
        }
    }

    private Order mapResultToEntity(Result result) {
        Order order = new Order();
        order.setOrderId(result.getValue("order_id").get().getAsString().get());
        order.setCustomerId(result.getValue("customer_id").get().getAsString().get());
        order.setStatus(result.getValue("status").get().getAsString().get());
        order.setCurrency(result.getValue("currency").get().getAsString().get());
        order.setCreatedAt(LocalDateTime.ofEpochSecond(Long.parseLong(result.getValue("created_at").get().getAsString().get()), 0, ZoneOffset.UTC));
        order.setUpdatedAt(LocalDateTime.ofEpochSecond(Long.parseLong(result.getValue("updated_at").get().getAsString().get()), 0, ZoneOffset.UTC));

        if (result.getValue("total_amount").isPresent()) {
            order.setTotalAmount(new BigDecimal(result.getValue("total_amount").get().getAsString().get()));
        }
        if (result.getValue("payment_method").isPresent()) {
            order.setPaymentMethod(result.getValue("payment_method").get().getAsString().get());
        }
        if (result.getValue("shipping_address").isPresent()) {
            order.setShippingAddress(result.getValue("shipping_address").get().getAsString().get());
        }
        if (result.getValue("notes").isPresent()) {
            order.setNotes(result.getValue("notes").get().getAsString().get());
        }
        if (result.getValue("inventory_reservation_id").isPresent()) {
            order.setInventoryReservationId(result.getValue("inventory_reservation_id").get().getAsString().get());
        }
        if (result.getValue("payment_id").isPresent()) {
            order.setPaymentId(result.getValue("payment_id").get().getAsString().get());
        }
        if (result.getValue("shipment_id").isPresent()) {
            order.setShipmentId(result.getValue("shipment_id").get().getAsString().get());
        }

        return order;
    }
    
    private void createCustomerOrderIndex(Order order, DistributedTransaction transaction) throws ExecutionException, CrudConflictException, CrudException {
        Put indexPut = Put.newBuilder()
                .namespace(NAMESPACE)
                .table("orders_by_customer")
                .partitionKey(Key.ofText("customer_id", order.getCustomerId()))
                .clusteringKey(Key.ofText("order_id", order.getOrderId()))
                .textValue("status", order.getStatus())
                .textValue("created_at", String.valueOf(order.getCreatedAt().toEpochSecond(ZoneOffset.UTC)))
                .build();
        transaction.put(indexPut);
    }
    
    private void deleteCustomerOrderIndex(String customerId, String orderId, DistributedTransaction transaction) throws ExecutionException, CrudConflictException, CrudException {
        Delete indexDelete = Delete.newBuilder()
                .namespace(NAMESPACE)
                .table("orders_by_customer")
                .partitionKey(Key.ofText("customer_id", customerId))
                .clusteringKey(Key.ofText("order_id", orderId))
                .build();
        transaction.delete(indexDelete);
    }
}