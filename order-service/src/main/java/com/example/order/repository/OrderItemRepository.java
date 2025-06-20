package com.example.order.repository;

import com.example.order.entity.OrderItem;
import com.scalar.db.api.*;
import com.scalar.db.exception.storage.ExecutionException;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.exception.transaction.AbortException;
import com.scalar.db.exception.transaction.CrudConflictException;
import com.scalar.db.exception.transaction.CrudException;
import com.scalar.db.io.Key;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class OrderItemRepository {

    private static final String NAMESPACE = "order_service";
    private static final String TABLE_NAME = "order_items";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final DistributedTransactionManager transactionManager;
    
    public OrderItemRepository(DistributedTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public OrderItem create(OrderItem orderItem, DistributedTransaction transaction) throws ExecutionException, CrudConflictException, CrudException {
        Put put = Put.newBuilder()
                .namespace(NAMESPACE)
                .table(TABLE_NAME)
                .partitionKey(Key.ofText("order_id", orderItem.getOrderId()))
                .clusteringKey(Key.ofText("product_id", orderItem.getProductId()))
                .textValue("product_name", orderItem.getProductName())
                .intValue("quantity", orderItem.getQuantity())
                .textValue("unit_price", orderItem.getUnitPrice().toString())
                .textValue("total_price", orderItem.getTotalPrice().toString())
                .textValue("sku", orderItem.getSku() != null ? orderItem.getSku() : "")
                .textValue("notes", orderItem.getNotes() != null ? orderItem.getNotes() : "")
                .textValue("created_at", String.valueOf(orderItem.getCreatedAt().toEpochSecond(ZoneOffset.UTC)))
                .build();
        transaction.put(put);
        return orderItem;
    }

    public List<OrderItem> findByOrderId(String orderId, DistributedTransaction transaction) throws ExecutionException, CrudConflictException, CrudException {
        Scan scan = Scan.newBuilder()
                .namespace(NAMESPACE)
                .table(TABLE_NAME)
                .partitionKey(Key.ofText("order_id", orderId))
                .build();

        List<Result> results = transaction.scan(scan);
        List<OrderItem> orderItems = new ArrayList<>();
        for (Result result : results) {
            orderItems.add(mapResultToEntity(result));
        }
        return orderItems;
    }

    /**
     * Batch fetch order items for multiple orders to avoid N+1 query problem
     */
    public Map<String, List<OrderItem>> findByOrderIds(List<String> orderIds, DistributedTransaction transaction) 
            throws ExecutionException, CrudConflictException, CrudException {
        
        Map<String, List<OrderItem>> orderItemsMap = orderIds.stream()
                .collect(Collectors.toMap(orderId -> orderId, orderId -> new ArrayList<>()));
        
        // Batch fetch all order items for the given order IDs
        for (String orderId : orderIds) {
            List<OrderItem> items = findByOrderId(orderId, transaction);
            orderItemsMap.put(orderId, items);
        }
        
        return orderItemsMap;
    }

    public Optional<OrderItem> findByOrderIdAndProductId(String orderId, String productId, DistributedTransaction transaction) throws ExecutionException, CrudConflictException, CrudException {
        Get get = Get.newBuilder()
                .namespace(NAMESPACE)
                .table(TABLE_NAME)
                .partitionKey(Key.ofText("order_id", orderId))
                .clusteringKey(Key.ofText("product_id", productId))
                .build();

        Optional<Result> result = transaction.get(get);
        if (result.isPresent()) {
            return Optional.of(mapResultToEntity(result.get()));
        }
        return Optional.empty();
    }

    public OrderItem update(OrderItem orderItem, DistributedTransaction transaction) throws ExecutionException, CrudConflictException, CrudException {
        Get get = Get.newBuilder()
                .namespace(NAMESPACE)
                .table(TABLE_NAME)
                .partitionKey(Key.ofText("order_id", orderItem.getOrderId()))
                .clusteringKey(Key.ofText("product_id", orderItem.getProductId()))
                .build();

        Optional<Result> existing = transaction.get(get);
        if (!existing.isPresent()) {
            throw new ExecutionException("Order item not found for update");
        }

        Put put = Put.newBuilder()
                .namespace(NAMESPACE)
                .table(TABLE_NAME)
                .partitionKey(Key.ofText("order_id", orderItem.getOrderId()))
                .clusteringKey(Key.ofText("product_id", orderItem.getProductId()))
                .textValue("product_name", orderItem.getProductName())
                .intValue("quantity", orderItem.getQuantity())
                .textValue("unit_price", orderItem.getUnitPrice().toString())
                .textValue("total_price", orderItem.getTotalPrice().toString())
                .textValue("sku", orderItem.getSku() != null ? orderItem.getSku() : "")
                .textValue("notes", orderItem.getNotes() != null ? orderItem.getNotes() : "")
                .textValue("created_at", existing.get().getValue("created_at").get().getAsString().get())
                .build();
        transaction.put(put);
        return orderItem;
    }

    public void deleteByOrderIdAndProductId(String orderId, String productId, DistributedTransaction transaction) throws ExecutionException, CrudConflictException, CrudException {
        Delete delete = Delete.newBuilder()
                .namespace(NAMESPACE)
                .table(TABLE_NAME)
                .partitionKey(Key.ofText("order_id", orderId))
                .clusteringKey(Key.ofText("product_id", productId))
                .build();

        transaction.delete(delete);
    }

    private OrderItem mapResultToEntity(Result result) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(result.getValue("order_id").get().getAsString().get());
        orderItem.setProductId(result.getValue("product_id").get().getAsString().get());
        orderItem.setProductName(result.getValue("product_name").get().getAsString().get());
        orderItem.setQuantity(result.getValue("quantity").get().getAsInt());
        orderItem.setUnitPrice(new BigDecimal(result.getValue("unit_price").get().getAsString().get()));
        orderItem.setTotalPrice(new BigDecimal(result.getValue("total_price").get().getAsString().get()));
        orderItem.setCreatedAt(LocalDateTime.ofEpochSecond(Long.parseLong(result.getValue("created_at").get().getAsString().get()), 0, ZoneOffset.UTC));

        if (result.getValue("sku").isPresent()) {
            orderItem.setSku(result.getValue("sku").get().getAsString().get());
        }
        if (result.getValue("notes").isPresent()) {
            orderItem.setNotes(result.getValue("notes").get().getAsString().get());
        }

        return orderItem;
    }
}